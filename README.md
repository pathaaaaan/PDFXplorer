# PDFXplorer

A modern, feature-rich PDF viewer built with JavaFX and Python integration.

## Features

- Smooth PDF rendering with high quality output
- Intuitive zoom controls (keyboard shortcuts and UI)
- Page navigation with thumbnails
- Fit width and fit page viewing modes
- Document information display
- Recent files history
- Keyboard shortcuts for common operations

## Requirements

### Java

- Java 11 or higher
- JavaFX

### Python

- Python 3.7 or higher
- PyMuPDF (fitz)

## Installation

1. Clone the repository:

```bash
git clone https://github.com/pathaaaaan/PDFXplorer.git
cd PDFXplorer
```

2. Set up Python virtual environment:

```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install PyMuPDF
```

3. Build with Maven:

```bash
mvn clean install
```

## Usage

### Running the Application

```bash
mvn javafx:run
```

### Keyboard Shortcuts

- **Zoom Controls:**

  - Command/Ctrl + Plus (+): Zoom in
  - Command/Ctrl + Minus (-): Zoom out
  - Command/Ctrl + 0: Reset zoom to 100%

- **Navigation:**
  - Left Arrow / Page Up: Previous page
  - Right Arrow / Page Down: Next page
  - Home: Go to first page
  - End: Go to last page

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- PyMuPDF for PDF rendering
- JavaFX for the UI framework
