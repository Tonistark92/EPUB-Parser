package com.iscoding.epubparser

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.iscoding.epubparser.models.Chapter
import com.iscoding.epubparser.ui.theme.EpubParserTheme
import com.iscoding.epubparser.util.extractAndParseEpub
import com.iscoding.epubparser.util.getPackageOpfPath
import com.iscoding.epubparser.util.listFilesInDirectory
import com.iscoding.epubparser.util.parsePackageOpf
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val epubFileName = "book.epub"
        val extractedDir = File(filesDir, "extracted_epub")
//        extractAndParseEpub(applicationContext, "book.epub")

        // Extract and parse the EPUB file
        val bookDir = extractAndParseEpub(this, epubFileName)

        bookDir?.let {
            val internalStorageDir = applicationContext.filesDir

            val bookDir = File(internalStorageDir, "book.epub")

            val tocNcxFile = File("/data/user/0/com.iscoding.epubparser/files/A Joyous Adventure/OEBPS/toc.ncx")
            val coverImage = File("/data/user/0/com.iscoding.epubparser/files/A Joyous Adventure/")

            val tocNcxPath = tocNcxFile.absolutePath
            val coverImagePath = coverImage.absolutePath
            viewModel.loadChapters(File(tocNcxPath))
            viewModel.loadBookImage(File(coverImagePath))

            val containerFile = File(it, "META-INF/container.xml")
            if (containerFile.exists()) {
                val packageOpfPath = getPackageOpfPath(containerFile)
                if (packageOpfPath != null) {
                    val packageOpfFile = File(it, packageOpfPath)
                    if (packageOpfFile.exists()) {
                        parsePackageOpf(packageOpfFile, it)
                    } else {
                        Log.d("EPUBParser", "package.opf file not found at $packageOpfPath")
                    }
                } else {
                    Log.d("EPUBParser", "Package path not found in container.xml")
                }
            } else {
                Log.d("EPUBParser", "container.xml not found in META-INF")
            }
        }
        setContent {
            MainScreen(viewModel)
        }
    }
    }

@Composable
fun MainScreen(viewModel: MainViewModel )  {
    val chapters by viewModel.chapters.observeAsState(emptyList())
    val chapterContent by viewModel.chapterContent.observeAsState("")
    val coverImagePath by viewModel.coverImagePath.observeAsState("")
    val context = LocalContext.current

    if (chapters.isEmpty()) {
        CircularProgressIndicator()
    } else {
        Column {
            if (coverImagePath.isNotEmpty()) {
                CoverImage(coverImagePath)
            }
            if (chapterContent.isEmpty()) {
                TocScreen(chapters, viewModel) { chapter ->
                    viewModel.loadChapterContent(chapter, File("/data/user/0/com.iscoding.epubparser/files/A Joyous Adventure/"))
                }
            } else {
                ChapterContentScreen(chapterContent)
            }
        }
    }
}
@Composable
fun CoverImage(imagePath: String) {
    val imageBitmap = remember { BitmapFactory.decodeFile(imagePath)?.asImageBitmap() }
    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun TocScreen(chapters: List<Chapter>,viewModel: MainViewModel , onChapterClick: (Chapter) -> Unit) {

    LazyColumn {
        item {
            val coverImagePath by viewModel.coverImagePath.observeAsState("")

            CoverImage(coverImagePath)

        }
        items(chapters) { chapter ->
            ChapterItem(chapter, onChapterClick)
        }
    }
}

@Composable
fun ChapterItem(chapter: Chapter, onChapterClick: (Chapter) -> Unit) {
    Text(
        text = chapter.title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChapterClick(chapter) }
            .padding(16.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun ChapterContentScreen(content: String) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
        }
    })
}