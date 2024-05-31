package com.iscoding.epubparser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iscoding.epubparser.models.Chapter
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory


class MainViewModel : ViewModel() {

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> get() = _chapters

    private val _chapterContent = MutableLiveData<String>()
    val chapterContent: LiveData<String> get() = _chapterContent

    fun loadChapters(tocFile: File) {
        val parsedChapters = parseNcxFile(tocFile)
        _chapters.value = parsedChapters
    }

    fun loadChapterContent(chapter: Chapter, bookDir: File) {
        val chapterFile = File("/data/user/0/com.iscoding.epubparser/files/A Joyous Adventure/OEBPS/${chapter.fileName}")
        val content = chapterFile.readText()
        _chapterContent.value = content
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
}