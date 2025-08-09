# PDF Viewer

A modern web application for viewing PDF documents with a clean, professional interface. Built with Flask and Bootstrap 5.

## Features

- 📤 Upload PDF files via drag-and-drop or file browser
- 📑 View PDFs directly in the browser
- 🔍 Zoom in/out functionality
- 🌓 Dark/Light mode support
- 📱 Responsive design
- 🗑️ Delete uploaded files
- 💾 Remembers last viewed file
- 🔒 Secure file handling

## Requirements

- Python 3.8+
- Flask
- PyMuPDF
- Modern web browser

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd pdf-viewer
```

2. Create a virtual environment and activate it:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

## Usage

1. Start the Flask server:
```bash
python app.py
```

2. Open your browser and navigate to:
```
http://localhost:5000
```

3. Upload PDF files by:
   - Dragging and dropping files onto the upload zone
   - Clicking the upload zone to select files

4. Click on any file in the sidebar to view it
5. Use the zoom controls to adjust the view
6. Toggle between dark and light mode using the theme button

## Security Notes

- Maximum file size: 10MB
- Only PDF files are allowed
- Files are stored securely in the `uploads` directory
- Filenames are sanitized before storage

## License

MIT License 