# EPUB Parser

This project is an EPUB parser that extracts and processes the contents of EPUB files. It handles unzipping the EPUB file, parsing all files within it, parsing the Table of Contents (TOC) for navigation, applying CSS styles, and extracting the cover image. However, the current implementation does not yet support displaying images and links within the chapter content.

## ⚠️ **Warning: This library is still under development and not finished yet!** ⚠️

## Features

- Unzip EPUB files and extract their contents
- Parse all files within the EPUB
- Parse the TOC file for navigation
- Apply CSS styles to the content
- Extract and display the cover image

## Not Implemented Yet

- Display images within the chapter content
- Display links within the chapter content

## Libraries Used

- `java.io.File`
- `java.io.FileOutputStream`
- `java.io.InputStream`
- `java.io.IOException`
- `javax.xml.parsers.DocumentBuilderFactory`
- `java.io.BufferedOutputStream`
- `java.util.zip.ZipFile`
- `org.w3c.dom.Document`
- `org.w3c.dom.Element`

## Screenshots
![epub1](https://github.com/Tonistark92/EPUB-Parser/assets/86676102/de1c7344-7644-46af-bff1-01aa148be1ab)
![epub2](https://github.com/Tonistark92/EPUB-Parser/assets/86676102/ced79e97-bd51-4f8b-b9e6-31cb33974a2b)


## Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- An IDE or text editor for Java development

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/epub-parser.git
   cd epub-parser
