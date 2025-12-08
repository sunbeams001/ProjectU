package com.projectu.shared.data.util

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import kotlin.time.ExperimentalTime
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * EPUB 生成器
 *
 * 使用 Okio 创建符合 EPUB 3.0 标准的电子书文件
 * EPUB 本质是一个 ZIP 文件，包含特定结构的 HTML/XHTML 文件和元数据
 *
 * 基本结构：
 * - mimetype (未压缩，必须是第一个文件)
 * - META-INF/container.xml
 * - OEBPS/content.opf (元数据和清单)
 * - OEBPS/toc.ncx (导航)
 * - OEBPS/Text/[*].xhtml (章节内容)
 * - OEBPS/Images/[*] (图片)
 * - OEBPS/Styles/style.css (样式)
 */
@OptIn(ExperimentalTime::class)
class EpubBuilder(
    private val fileSystem: FileSystem,
    private val outputPath: Path? = null,
    private val outputSink: BufferedSink? = null
) {
    private val chapters = mutableListOf<Chapter>()
    private val images = mutableListOf<Image>()
    private var metadata: Metadata? = null
    
    // 记录所有 ZIP 条目信息用于生成 Central Directory
    private data class ZipEntry(
        val fileName: String,
        val localHeaderOffset: Long,
        val crc32: Long,
        val compressedSize: Int,
        val uncompressedSize: Int,
        val compressionMethod: Int
    )
    
    /**
     * 章节数据
     */
    data class Chapter(
        val id: String,
        val title: String,
        val htmlContent: String,
        val order: Int
    )
    
    /**
     * 图片数据
     */
    data class Image(
        val id: String,
        val fileName: String,
        val data: ByteArray,
        val mimeType: String = "image/jpeg"
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Image
            return id == other.id && fileName == other.fileName && mimeType == other.mimeType
        }
        
        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }
    
    /**
     * 元数据
     */
    data class Metadata(
        val title: String,
        val author: String,
        val language: String = "ja",
        val identifier: String,
        val publisher: String = "Pixiv",
        val description: String? = null,
        val coverImageId: String? = null
    )
    
    /**
     * 设置元数据
     */
    fun setMetadata(metadata: Metadata) {
        this.metadata = metadata
    }
    
    /**
     * 添加章节
     */
    fun addChapter(chapter: Chapter) {
        chapters.add(chapter)
    }
    
    /**
     * 添加图片
     */
    fun addImage(image: Image) {
        images.add(image)
    }
    
    /**
     * 构建并保存 EPUB 文件
     */
    fun build() {
        val meta = metadata ?: throw IllegalStateException("Metadata not set")
        
        // 根据提供的参数选择输出方式
        when {
            outputSink != null -> {
                // 使用提供的 Sink（支持 PlatformFileWriter）
                buildToSink(outputSink, meta)
            }
            outputPath != null -> {
                // 使用 FileSystem 直接写入（Desktop/测试）
                fileSystem.write(outputPath) {
                    buildToSink(this, meta)
                }
            }
            else -> {
                throw IllegalStateException("Either outputPath or outputSink must be provided")
            }
        }
    }
    
    /**
     * 构建 EPUB 内容到指定的 Sink
     */
    private fun buildToSink(sink: BufferedSink, meta: Metadata) {
        val zipEntries = mutableListOf<ZipEntry>()
        var currentOffset = 0L
        
        // 1. mimetype (必须未压缩，且是第一个文件)
        currentOffset = writeMimetypeEntry(sink, zipEntries, currentOffset)
        
        // 2. META-INF/container.xml
        currentOffset = writeContainerEntry(sink, zipEntries, currentOffset)
        
        // 3. OEBPS/nav.xhtml (EPUB 3 导航文档)
        currentOffset = writeNavEntry(sink, meta, zipEntries, currentOffset)
        
        // 4. OEBPS/content.opf
        currentOffset = writeContentOpfEntry(sink, meta, zipEntries, currentOffset)
        
        // 5. OEBPS/toc.ncx
        currentOffset = writeTocNcxEntry(sink, meta, zipEntries, currentOffset)
        
        // 6. OEBPS/Styles/style.css
        currentOffset = writeStyleCssEntry(sink, zipEntries, currentOffset)
        
        // 6. 章节内容
        chapters.forEach { chapter ->
            currentOffset = writeChapterEntry(sink, chapter, zipEntries, currentOffset)
        }
        
        // 7. 图片
        images.forEach { image ->
            currentOffset = writeImageEntry(sink, image, zipEntries, currentOffset)
        }
        
        // 8. 写入 Central Directory
        val centralDirOffset = currentOffset
        zipEntries.forEach { entry ->
            currentOffset = writeCentralDirectoryHeader(sink, entry, currentOffset)
        }
        val centralDirSize = currentOffset - centralDirOffset
        
        // 9. 写入 End of Central Directory
        writeEndOfCentralDirectory(sink, zipEntries.size, centralDirSize.toInt(), centralDirOffset.toInt())
        
        // 确保数据全部写入
        sink.flush()
    }
    
    /**
     * 写入 mimetype 文件（未压缩）
     * @return 新的偏移量
     */
    private fun writeMimetypeEntry(sink: BufferedSink, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val content = "application/epub+zip".encodeToByteArray()
        val fileName = "mimetype"
        val fileNameBytes = fileName.encodeToByteArray()
        
        val crc = CRC32()
        crc.update(content)
        
        // ZIP Local File Header (30 bytes + fileName + content)
        sink.writeIntLe(0x04034b50) // 签名 (4 bytes)
        sink.writeShortLe(20) // 版本 (2 bytes)
        sink.writeShortLe(0) // 标志 (2 bytes)
        sink.writeShortLe(0) // 压缩方法 (2 bytes)
        sink.writeShortLe(0) // 修改时间 (2 bytes)
        sink.writeShortLe(0) // 修改日期 (2 bytes)
        sink.writeIntLe(crc.value.toInt()) // CRC-32 (4 bytes)
        sink.writeIntLe(content.size) // 压缩后大小 (4 bytes)
        sink.writeIntLe(content.size) // 未压缩大小 (4 bytes)
        sink.writeShortLe(fileNameBytes.size) // 文件名长度 (2 bytes)
        sink.writeShortLe(0) // 扩展字段长度 (2 bytes)
        sink.write(fileNameBytes) // 文件名
        sink.write(content) // 内容
        
        zipEntries.add(ZipEntry(
            fileName = fileName,
            localHeaderOffset = offset,
            crc32 = crc.value,
            compressedSize = content.size,
            uncompressedSize = content.size,
            compressionMethod = 0
        ))
        
        return offset + 30 + fileNameBytes.size + content.size
    }
    
    /**
     * 写入 META-INF/container.xml
     * @return 新的偏移量
     */
    private fun writeContainerEntry(sink: BufferedSink, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
    <rootfiles>
        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
    </rootfiles>
</container>"""
        
        return writeCompressedZipEntry(sink, "META-INF/container.xml", xml.encodeToByteArray(), zipEntries, offset)
    }
    
    /**
     * 写入 nav.xhtml（EPUB 3 导航文档）
     * @return 新的偏移量
     */
    private fun writeNavEntry(sink: BufferedSink, meta: Metadata, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val navItems = chapters.mapIndexed { index, chapter ->
            """        <li><a href="Text/${chapter.id}.xhtml">${escapeXml(chapter.title)}</a></li>"""
        }.joinToString("\n")
        
        val xhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="ja" lang="ja">
<head>
    <meta charset="UTF-8"/>
    <title>目次</title>
    <link rel="stylesheet" type="text/css" href="Styles/style.css"/>
</head>
<body>
    <nav epub:type="toc" id="toc">
        <h1>目次</h1>
        <ol>
$navItems
        </ol>
    </nav>
</body>
</html>"""
        
        return writeCompressedZipEntry(sink, "OEBPS/nav.xhtml", xhtml.encodeToByteArray(), zipEntries, offset)
    }
    
    /**
     * 写入 content.opf（包含元数据、清单、书脊）
     * @return 新的偏移量
     */
    private fun writeContentOpfEntry(sink: BufferedSink, meta: Metadata, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val timestamp = "${now.year}-${now.month.number.toString().padStart(2, '0')}-${now.day.toString().padStart(2, '0')}T${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}Z"
        
        val manifestItems = buildString {
            // NCX
            appendLine("""    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>""")
            
            // Nav (EPUB 3 导航文档)
            appendLine("""    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""")
            
            // CSS
            appendLine("""    <item id="css" href="Styles/style.css" media-type="text/css"/>""")
            
            // 章节
            chapters.forEach { chapter ->
                appendLine("""    <item id="${chapter.id}" href="Text/${chapter.id}.xhtml" media-type="application/xhtml+xml"/>""")
            }
            
            // 图片
            images.forEach { image ->
                appendLine("""    <item id="${image.id}" href="Images/${image.fileName}" media-type="${image.mimeType}"/>""")
            }
        }
        
        val spineItems = chapters.joinToString("\n") { chapter ->
            """    <itemref idref="${chapter.id}"/>"""
        }
        
        val descriptionTag = meta.description?.let {
            """<dc:description>${escapeXml(it)}</dc:description>"""
        } ?: ""
        
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="BookId">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
        <dc:identifier id="BookId">${escapeXml(meta.identifier)}</dc:identifier>
        <dc:title>${escapeXml(meta.title)}</dc:title>
        <dc:creator>${escapeXml(meta.author)}</dc:creator>
        <dc:language>${meta.language}</dc:language>
        <dc:publisher>${escapeXml(meta.publisher)}</dc:publisher>
        <dc:date>$timestamp</dc:date>
        $descriptionTag
        <meta property="dcterms:modified">$timestamp</meta>
    </metadata>
    <manifest>
$manifestItems
    </manifest>
    <spine toc="ncx">
$spineItems
    </spine>
</package>"""
        
        return writeCompressedZipEntry(sink, "OEBPS/content.opf", xml.encodeToByteArray(), zipEntries, offset)
    }
    
    /**
     * 写入 toc.ncx（目录导航）
     * @return 新的偏移量
     */
    private fun writeTocNcxEntry(sink: BufferedSink, meta: Metadata, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val navPoints = chapters.mapIndexed { index, chapter ->
            """
                <navPoint id="navPoint-${index + 1}" playOrder="${index + 1}">
                    <navLabel>
                        <text>${escapeXml(chapter.title)}</text>
                    </navLabel>
                    <content src="Text/${chapter.id}.xhtml"/>
                </navPoint>
            """.trimIndent()
        }.joinToString("\n")
        
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
    <head>
        <meta name="dtb:uid" content="${escapeXml(meta.identifier)}"/>
        <meta name="dtb:depth" content="1"/>
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
    </head>
    <docTitle>
        <text>${escapeXml(meta.title)}</text>
    </docTitle>
    <navMap>
$navPoints
    </navMap>
</ncx>"""
        
        return writeCompressedZipEntry(sink, "OEBPS/toc.ncx", xml.encodeToByteArray(), zipEntries, offset)
    }
    
    /**
     * 写入样式文件
     * @return 新的偏移量
     */
    private fun writeStyleCssEntry(sink: BufferedSink, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val css = """body {
    font-family: "Hiragino Mincho ProN", "Yu Mincho", "YuMincho", "MS Mincho", serif;
    line-height: 1.8;
    margin: 0;
    padding: 1em;
    text-align: justify;
}

h1, h2 {
    text-align: center;
    margin: 2em 0 1em 0;
    font-weight: bold;
}

h1 {
    font-size: 1.5em;
}

h2 {
    font-size: 1.3em;
}

p {
    text-indent: 1em;
    margin: 0.5em 0;
}

.chapter-title {
    text-align: center;
    font-size: 1.3em;
    font-weight: bold;
    margin: 2em 0;
}

.image {
    text-align: center;
    margin: 1em 0;
}

.image img {
    max-width: 100%;
    height: auto;
}

.link {
    color: #0066cc;
    text-decoration: underline;
}

ruby {
    ruby-position: over;
}

rt {
    font-size: 0.6em;
}"""
        
        return writeCompressedZipEntry(sink, "OEBPS/Styles/style.css", css.encodeToByteArray(), zipEntries, offset)
    }
    
    /**
     * 写入章节内容
     * @return 新的偏移量
     */
    private fun writeChapterEntry(sink: BufferedSink, chapter: Chapter, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val xhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="ja" lang="ja">
<head>
    <meta charset="UTF-8"/>
    <title>${escapeXml(chapter.title)}</title>
    <link rel="stylesheet" type="text/css" href="../Styles/style.css"/>
</head>
<body>
${chapter.htmlContent}
</body>
</html>"""
        
        return writeCompressedZipEntry(sink, "OEBPS/Text/${chapter.id}.xhtml", xhtml.encodeToByteArray(), zipEntries, offset)
    }
    
    /**
     * 写入图片
     * @return 新的偏移量
     */
    private fun writeImageEntry(sink: BufferedSink, image: Image, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        return writeCompressedZipEntry(sink, "OEBPS/Images/${image.fileName}", image.data, zipEntries, offset)
    }
    
    /**
     * 写入一个压缩的 ZIP 条目
     * @return 新的偏移量
     */
    private fun writeCompressedZipEntry(sink: BufferedSink, fileName: String, data: ByteArray, zipEntries: MutableList<ZipEntry>, offset: Long): Long {
        val fileNameBytes = fileName.encodeToByteArray()
        
        // 压缩数据
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        deflater.setInput(data)
        deflater.finish()
        
        val compressedBuffer = ByteArray(data.size + 1024)
        val compressedSize = deflater.deflate(compressedBuffer)
        deflater.end()
        
        // 计算 CRC32
        val crc = CRC32()
        crc.update(data)
        
        // Local File Header (30 bytes + fileName + compressedData)
        sink.writeIntLe(0x04034b50) // 签名 (4 bytes)
        sink.writeShortLe(20) // 版本 (2 bytes)
        sink.writeShortLe(0) // 标志 (2 bytes)
        sink.writeShortLe(8) // 压缩方法（8 = DEFLATE）(2 bytes)
        sink.writeShortLe(0) // 修改时间 (2 bytes)
        sink.writeShortLe(0) // 修改日期 (2 bytes)
        sink.writeIntLe(crc.value.toInt()) // CRC-32 (4 bytes)
        sink.writeIntLe(compressedSize) // 压缩后大小 (4 bytes)
        sink.writeIntLe(data.size) // 未压缩大小 (4 bytes)
        sink.writeShortLe(fileNameBytes.size) // 文件名长度 (2 bytes)
        sink.writeShortLe(0) // 扩展字段长度 (2 bytes)
        sink.write(fileNameBytes) // 文件名
        sink.write(compressedBuffer, 0, compressedSize) // 压缩数据
        
        zipEntries.add(ZipEntry(
            fileName = fileName,
            localHeaderOffset = offset,
            crc32 = crc.value,
            compressedSize = compressedSize,
            uncompressedSize = data.size,
            compressionMethod = 8
        ))
        
        return offset + 30 + fileNameBytes.size + compressedSize
    }
    
    /**
     * 写入 Central Directory Header
     * @return 新的偏移量
     */
    private fun writeCentralDirectoryHeader(sink: BufferedSink, entry: ZipEntry, offset: Long): Long {
        val fileNameBytes = entry.fileName.encodeToByteArray()
        
        // Central Directory Header (46 bytes + fileName)
        sink.writeIntLe(0x02014b50) // Central Directory 签名 (4 bytes)
        sink.writeShortLe(20) // 创建版本 (2 bytes)
        sink.writeShortLe(20) // 解压所需版本 (2 bytes)
        sink.writeShortLe(0) // 标志 (2 bytes)
        sink.writeShortLe(entry.compressionMethod) // 压缩方法 (2 bytes)
        sink.writeShortLe(0) // 修改时间 (2 bytes)
        sink.writeShortLe(0) // 修改日期 (2 bytes)
        sink.writeIntLe(entry.crc32.toInt()) // CRC-32 (4 bytes)
        sink.writeIntLe(entry.compressedSize) // 压缩后大小 (4 bytes)
        sink.writeIntLe(entry.uncompressedSize) // 未压缩大小 (4 bytes)
        sink.writeShortLe(fileNameBytes.size) // 文件名长度 (2 bytes)
        sink.writeShortLe(0) // 扩展字段长度 (2 bytes)
        sink.writeShortLe(0) // 文件注释长度 (2 bytes)
        sink.writeShortLe(0) // 磁盘编号 (2 bytes)
        sink.writeShortLe(0) // 内部文件属性 (2 bytes)
        sink.writeIntLe(0) // 外部文件属性 (4 bytes)
        sink.writeIntLe(entry.localHeaderOffset.toInt()) // Local Header 偏移 (4 bytes)
        sink.write(fileNameBytes) // 文件名
        
        return offset + 46 + fileNameBytes.size
    }
    
    /**
     * 写入 End of Central Directory Record
     */
    private fun writeEndOfCentralDirectory(sink: BufferedSink, entryCount: Int, centralDirSize: Int, centralDirOffset: Int) {
        sink.writeIntLe(0x06054b50) // End of Central Directory 签名
        sink.writeShortLe(0) // 当前磁盘编号
        sink.writeShortLe(0) // Central Directory 起始磁盘
        sink.writeShortLe(entryCount) // 当前磁盘上的条目数
        sink.writeShortLe(entryCount) // 总条目数
        sink.writeIntLe(centralDirSize) // Central Directory 大小
        sink.writeIntLe(centralDirOffset) // Central Directory 偏移
        sink.writeShortLe(0) // ZIP 文件注释长度
    }
    
    /**
     * 转义 XML 特殊字符
     */
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
