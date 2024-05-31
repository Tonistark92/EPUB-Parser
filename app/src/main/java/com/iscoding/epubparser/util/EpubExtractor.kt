package com.iscoding.epubparser.util

import android.content.Context
import android.util.Log
import com.iscoding.epubparser.models.Chapter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import java.io.BufferedOutputStream
import java.util.zip.ZipFile
import org.w3c.dom.Document
import org.w3c.dom.Element

fun copyEpubFromAssets(context: Context, epubFileName: String, destinationPath: String) {
    val assetManager = context.assets
    val inputStream: InputStream = assetManager.open(epubFileName)
    val outFile = File(destinationPath)
    val outputStream: FileOutputStream = FileOutputStream(outFile)

    val buffer = ByteArray(1024)
    var length: Int
    while (inputStream.read(buffer).also { length = it } > 0) {
        outputStream.write(buffer, 0, length)
    }

    outputStream.close()
    inputStream.close()
}

fun extractAndParseEpub(context: Context, epubFileName: String): File? {
    // Define paths
    val destinationPath = context.filesDir.absolutePath + File.separator + epubFileName
    val tempExtractedDir = File(context.filesDir, "temp_extracted_epub")

    // Copy EPUB from assets to internal storage
    copyEpubFromAssets(context, epubFileName, destinationPath)

    // Unzip the EPUB file
    try {
        UnzipUtils.unzip(File(destinationPath), tempExtractedDir.absolutePath)
        Log.d("EPUBParser", "EPUB extracted to: ${tempExtractedDir.absolutePath}")
        listFilesRecursively(tempExtractedDir)

        // Parse metadata to get book title and create a directory
        val bookTitle = parseEpubMetadata(tempExtractedDir)
        if (bookTitle != null) {
            val bookDir = File(context.filesDir, bookTitle)
            if (!bookDir.exists()) {
                bookDir.mkdir()
            }
            tempExtractedDir.copyRecursively(bookDir, true)
            tempExtractedDir.deleteRecursively()
            if (tempExtractedDir.isDirectory) {
                Log.e("EPUBParser", "tempExtractedDir is AAAAAAAAAA directory")

            } else {
                Log.e("EPUBParser", "tempExtractedDir is not a directory")
            }
            Log.d("EPUBParser", "Book files moved to: ${bookDir.absolutePath}")

            return bookDir
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

fun listFilesRecursively(directory: File) {
    directory.walkTopDown().forEach { file ->
        Log.d("EPUBParser", "Extracted file: ${file.absolutePath}")
    }
}

fun parseEpubMetadata(epubDir: File): String? {
    // Check for both content.opf and package.opf
    val contentOpf = File(epubDir, "OEBPS/content.opf")
    val packageOpf = File(epubDir, "OEBPS/package.opf")

    val opfFile = when {
        contentOpf.exists() -> contentOpf
        packageOpf.exists() -> packageOpf
        else -> {
            Log.d("EPUBParser", "Neither content.opf nor package.opf found!")
            return null
        }
    }

    Log.d("EPUBParser", "Using OPF file: ${opfFile.absolutePath}")

    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = documentBuilder.parse(opfFile)

    // Normalize XML structure
    doc.documentElement.normalize()

    // Example: Read book title
    val titleNodeList = doc.getElementsByTagName("dc:title")
    val title = if (titleNodeList.length > 0) {
        titleNodeList.item(0).textContent
    } else {
        "Untitled Book"
    }

    Log.d("EPUBParser", "Title: $title")

    // Example: Read book author
    val creatorNodeList = doc.getElementsByTagName("dc:creator")
    if (creatorNodeList.length > 0) {
        val author = creatorNodeList.item(0).textContent
        Log.d("EPUBParser", "Author: $author")
    }

    // Add more parsing as needed

    return title
}

fun listFilesInDirectory(directory: File): List<File> {
    val filesList = mutableListOf<File>()
    directory.walkTopDown().forEach { file ->
        if (file.isFile) {
            filesList.add(file)
        }
    }
    return filesList
}

/**
 * UnzipUtils class extracts files and sub-directories of a standard zip file to
 * a destination directory.
 *
 */
object UnzipUtils {
    /**
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    @Throws(IOException::class)
    fun unzip(zipFilePath: File, destDirectory: String) {

        File(destDirectory).run {
            if (!exists()) {
                mkdirs()
            }
        }

        ZipFile(zipFilePath).use { zip ->

            zip.entries().asSequence().forEach { entry ->

                zip.getInputStream(entry).use { input ->

                    val filePath = destDirectory + File.separator + entry.name

                    if (!entry.isDirectory) {
                        // if the entry is a file, extracts it
                        extractFile(input, filePath)
                        Log.d("EPUBParser", "File extracted: $filePath")
                    } else {
                        // if the entry is a directory, make the directory
                        val dir = File(filePath)
                        dir.mkdir()
                        Log.d("EPUBParser", "Directory created: $filePath")
                    }

                }

            }
        }
    }

    /**
     * Extracts a zip entry (file entry)
     * @param inputStream
     * @param destFilePath
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        val bos = BufferedOutputStream(FileOutputStream(destFilePath))
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (inputStream.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }

    /**
     * Size of the buffer to read/write data
     */
    private const val BUFFER_SIZE = 4096
}
////parsing
//fun parsePackageOpf(packageOpfFile: File) {
//    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//    val doc: Document = documentBuilder.parse(packageOpfFile)
//    doc.documentElement.normalize()
//
//    // Example: Read book title
//    val titleNodeList = doc.getElementsByTagName("dc:title")
//    if (titleNodeList.length > 0) {
//        val title = titleNodeList.item(0).textContent
//        Log.d("XMLParser", "Title: $title")
//    }
//
//    // Example: Read book author
//    val creatorNodeList = doc.getElementsByTagName("dc:creator")
//    if (creatorNodeList.length > 0) {
//        val author = creatorNodeList.item(0).textContent
//        Log.d("XMLParser", "Author: $author")
//    }
//
//    // Example: Read manifest items
//    val manifestNodeList = doc.getElementsByTagName("manifest").item(0).childNodes
//    for (i in 0 until manifestNodeList.length) {
//        val item = manifestNodeList.item(i)
//        if (item is org.w3c.dom.Element) {
//            val id = item.getAttribute("id")
//            val href = item.getAttribute("href")
//            val mediaType = item.getAttribute("media-type")
//            Log.d("XMLParser", "Manifest item - id: $id, href: $href, mediaType: $mediaType")
//        }
//    }
//
//    // Add more parsing as needed
//}
 fun getPackageOpfPath(containerFile: File): String? {
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc: Document = documentBuilder.parse(containerFile)
    doc.documentElement.normalize()

    val rootfiles = doc.getElementsByTagName("rootfile")
    if (rootfiles.length > 0) {
        val rootfileElement = rootfiles.item(0) as Element
        return rootfileElement.getAttribute("full-path")
    }
    return null
}
fun parsePackageOpf(packageOpfFile: File, bookDir: File) {
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc: Document = documentBuilder.parse(packageOpfFile)
    doc.documentElement.normalize()

    parseMetadata(doc)
    val manifest = parseManifest(doc)
    parseSpine(doc, manifest)

    val tocNcxFile = File(bookDir, "OEBPS/toc.ncx")
    if (tocNcxFile.exists()) {
        parseTocNcx(tocNcxFile)
    } else {
        Log.d("EPUBParser", "toc.ncx not found!")
    }

///// wont use it
//    val bk01TocFile = File(bookDir, "OEBPS/bk01-toc.xhtml")
//    if (bk01TocFile.exists()) {
//        Log.d("EPUBParser", "bk01-toc.xhtml  ************************************************************************************")
//
//        parseXhtmlFile(bk01TocFile)
//    } else {
//        Log.d("EPUBParser", "bk01-toc.xhtml not found!")
//    }
//    val indexFile = File(bookDir, "OEBPS/index.xhtml")
//    if (indexFile.exists()) {
//        parseXhtmlFile(indexFile)
//    } else {
//        Log.d("EPUBParser", "index.xhtml not found!")
//    }

    val cssFile = File(bookDir, "OEBPS/docbook-epub.css")
    if (cssFile.exists()) {
        parseCssFile(cssFile)
        Log.d("EPUBParser", "tocNcxFile.xhtml  ************************************************************************************")

        parseCssFile(tocNcxFile)
    } else {
        Log.d("EPUBParser", "docbook-epub.css not found!")
    }
}
 fun parseMetadata(doc: Document) {
    val metadata = doc.getElementsByTagName("metadata").item(0) as Element

    // Example: Read book title
    val titleNodeList = metadata.getElementsByTagName("dc:title")
    if (titleNodeList.length > 0) {
        val title = titleNodeList.item(0).textContent
        Log.d("XMLParser", "Title: $title")
    }

    // Example: Read book author
    val creatorNodeList = metadata.getElementsByTagName("dc:creator")
    if (creatorNodeList.length > 0) {
        val author = creatorNodeList.item(0).textContent
        Log.d("XMLParser", "Author: $author")
    }

    // Read more metadata
    val identifierNodeList = metadata.getElementsByTagName("dc:identifier")
    if (identifierNodeList.length > 0) {
        val identifier = identifierNodeList.item(0).textContent
        Log.d("XMLParser", "Identifier: $identifier")
    }

    val languageNodeList = metadata.getElementsByTagName("dc:language")
    if (languageNodeList.length > 0) {
        val language = languageNodeList.item(0).textContent
        Log.d("XMLParser", "Language: $language")
    }

    val publisherNodeList = metadata.getElementsByTagName("dc:publisher")
    if (publisherNodeList.length > 0) {
        val publisher = publisherNodeList.item(0).textContent
        Log.d("XMLParser", "Publisher: $publisher")
    }

    val dateNodeList = metadata.getElementsByTagName("dc:date")
    if (dateNodeList.length > 0) {
        val date = dateNodeList.item(0).textContent
        Log.d("XMLParser", "Date: $date")
    }

    val descriptionNodeList = metadata.getElementsByTagName("dc:description")
    if (descriptionNodeList.length > 0) {
        val description = descriptionNodeList.item(0).textContent
        Log.d("XMLParser", "Description: $description")
    }

    val subjectNodeList = metadata.getElementsByTagName("dc:subject")
    if (subjectNodeList.length > 0) {
        val subject = subjectNodeList.item(0).textContent
        Log.d("XMLParser", "Subject: $subject")
    }

    val rightsNodeList = metadata.getElementsByTagName("dc:rights")
    if (rightsNodeList.length > 0) {
        val rights = rightsNodeList.item(0).textContent
        Log.d("XMLParser", "Rights: $rights")
    }

    val metaNodeList = metadata.getElementsByTagName("meta")
    for (i in 0 until metaNodeList.length) {
        val metaElement = metaNodeList.item(i) as Element
        val name = metaElement.getAttribute("name")
        val content = metaElement.getAttribute("content")
        val property = metaElement.getAttribute("property")
        if (name.isNotEmpty()) {
            Log.d("XMLParser", "Meta - name: $name, content: $content")
        }
        if (property.isNotEmpty()) {
            Log.d("XMLParser", "Meta - property: $property, content: $content")
        }
    }
}

 fun parseManifest(doc: Document): Map<String, String> {
    val manifestNodeList = doc.getElementsByTagName("manifest").item(0).childNodes
    val manifestMap = mutableMapOf<String, String>()
    for (i in 0 until manifestNodeList.length) {
        val item = manifestNodeList.item(i)
        if (item is Element) {
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            val mediaType = item.getAttribute("media-type")
            manifestMap[id] = href
            Log.d("XMLParser", "Manifest item - id: $id, href: $href, mediaType: $mediaType")
        }
    }
    return manifestMap
}

fun parseSpine(doc: Document, manifest: Map<String, String>) {
    val spineNodeList = doc.getElementsByTagName("spine").item(0).childNodes
    for (i in 0 until spineNodeList.length) {
        val item = spineNodeList.item(i)
        if (item is Element) {
            val idref = item.getAttribute("idref")
            val href = manifest[idref]
            Log.d("XMLParser", "Spine item - idref: $idref, href: $href")
            // You can add additional processing here if needed
        }
    }
}

fun parseTocNcx(tocNcxFile: File) {
    try {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = documentBuilder.parse(tocNcxFile)
        doc.documentElement.normalize()

        val navMapNodeList = doc.getElementsByTagName("navMap")
        if (navMapNodeList.length > 0) {
            val navPoints = navMapNodeList.item(0).childNodes
            for (i in 0 until navPoints.length) {
                val navPoint = navPoints.item(i)
                if (navPoint is Element) {
                    val id = navPoint.getAttribute("id")
                    val playOrder = navPoint.getAttribute("playOrder")
                    val navLabel = navPoint.getElementsByTagName("navLabel").item(0).textContent
                    val contentSrc = navPoint.getElementsByTagName("content").item(0).attributes.getNamedItem("src").nodeValue
                    Log.d("XMLParser", "TOC - id: $id, playOrder: $playOrder, navLabel: $navLabel, contentSrc: $contentSrc")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun parseNcxFile(ncxFile: File): List<Chapter> {
    val chapters = mutableListOf<Chapter>()
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc: Document = documentBuilder.parse(ncxFile)
    doc.documentElement.normalize()

    val navPoints = doc.getElementsByTagName("navPoint")
    for (i in 0 until navPoints.length) {
        val navPoint = navPoints.item(i) as Element
        val navLabel = navPoint.getElementsByTagName("navLabel").item(0) as Element
        val text = navLabel.getElementsByTagName("text").item(0).textContent
        val content = navPoint.getElementsByTagName("content").item(0) as Element
        val src = content.getAttribute("src")
        chapters.add(Chapter(text, src))
    }
    return chapters
}

//fun parseTocXhtmlFile(tocXhtmlFile: File): List<Chapter> {
//    val chapters = mutableListOf<Chapter>()
//    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//    val doc: Document = documentBuilder.parse(tocXhtmlFile)
//    doc.documentElement.normalize()
//
//    val tocItems = doc.getElementsByTagName("nav").item(0).getElementsByTagName("li")
//    for (i in 0 until tocItems.length) {
//        val item = tocItems.item(i) as Element
//        val link = item.getElementsByTagName("a").item(0).attributes.getNamedItem("href").nodeValue
//        val chapterTitle = item.textContent
//        chapters.add(Chapter(chapterTitle, link))
//    }
//    return chapters
//}
fun parseCssFile(cssFile: File) {
    if (cssFile.exists()) {
        val content = cssFile.readText()
        Log.d("XMLParser", "CSS Content: $content")
    } else {
        Log.d("XMLParser", "${cssFile.name} not found!")
    }
}