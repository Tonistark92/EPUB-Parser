package com.iscoding.epubparser

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iscoding.epubparser.models.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory


class MainViewModel : ViewModel() {
    private val _coverImagePath = MutableLiveData<String>()
    val coverImagePath: LiveData<String> get() = _coverImagePath

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> get() = _chapters

    private val _chapterContent = MutableLiveData<String>()
    val chapterContent: LiveData<String> get() = _chapterContent

    fun loadChapters(tocFile: File) {

            val parsedChapters = parseNcxFile(tocFile)
            _chapters.value = parsedChapters
    }

    fun loadBookImage(bookDir: File) {
        viewModelScope.launch(Dispatchers.IO) {

            val coverImagePath = loadCoverImagePath(bookDir)
            _coverImagePath.postValue(coverImagePath)
        }
    }

    private fun loadCoverImagePath(bookDir: File): String {
        // The cover image is assumed to be located in "OEBPS/bookcover-generated.jpg"
        return File(bookDir, "OEBPS/bookcover-generated.jpg").absolutePath
    }

    fun loadChapterContent(chapter: Chapter, bookDir: File) {
        val chapterFile = File("/data/user/0/com.iscoding.epubparser/files/A Joyous Adventure/OEBPS/${chapter.fileName}")
        val content = chapterFile.readText()
//        Log.d("HTMLParser", "HTML Chapter - ${chapterFile.readText()} ")
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