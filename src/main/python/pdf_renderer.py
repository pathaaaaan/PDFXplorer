import sys
import json
import fitz  # PyMuPDF
import base64
from io import BytesIO
from PIL import Image

def render_page(pdf_path, page_num, zoom=1.0):
    try:
        # Open the PDF
        doc = fitz.open(pdf_path)
        
        if not (0 <= page_num < doc.page_count):
            return json.dumps({
                "success": False,
                "error": f"Page number out of range. Total pages: {doc.page_count}"
            })

        # Get the page
        page = doc[page_num]
        
        # Calculate matrix for zoom
        matrix = fitz.Matrix(zoom, zoom)
        
        # Render page to pixmap
        pix = page.get_pixmap(matrix=matrix)
        
        # Convert to PIL Image
        img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
        
        # Save to bytes
        img_byte_arr = BytesIO()
        img.save(img_byte_arr, format='PNG')
        img_byte_arr = img_byte_arr.getvalue()
        
        # Convert to base64
        img_base64 = base64.b64encode(img_byte_arr).decode()
        
        # Get page info
        page_info = {
            "width": page.rect.width,
            "height": page.rect.height,
            "rotation": page.rotation,
            "rendered_width": pix.width,
            "rendered_height": pix.height
        }
        
        return json.dumps({
            "success": True,
            "image": img_base64,
            "page_info": page_info
        })
        
    except Exception as e:
        return json.dumps({
            "success": False,
            "error": str(e)
        })
    finally:
        if 'doc' in locals():
            doc.close()

def get_document_info(pdf_path):
    try:
        doc = fitz.open(pdf_path)
        info = {
            "page_count": doc.page_count,
            "metadata": doc.metadata,
            "is_encrypted": doc.is_encrypted,
            "page_sizes": [
                {"width": page.rect.width, "height": page.rect.height}
                for page in doc
            ]
        }
        return json.dumps({
            "success": True,
            "info": info
        })
    except Exception as e:
        return json.dumps({
            "success": False,
            "error": str(e)
        })
    finally:
        if 'doc' in locals():
            doc.close()

if __name__ == "__main__":
    command = sys.argv[1]
    pdf_path = sys.argv[2]
    
    if command == "render":
        page_num = int(sys.argv[3])
        zoom = float(sys.argv[4]) if len(sys.argv) > 4 else 1.0
        print(render_page(pdf_path, page_num, zoom))
    elif command == "info":
        print(get_document_info(pdf_path))
    else:
        print(json.dumps({
            "success": False,
            "error": f"Unknown command: {command}"
        })) 