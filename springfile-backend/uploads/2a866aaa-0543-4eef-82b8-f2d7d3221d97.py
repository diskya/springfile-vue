from pdf2docx import Converter

def convert_pdf_to_docx(pdf_file, docx_file):
    try:
        cv = Converter(pdf_file)
        cv.convert(docx_file)
        cv.close()
        print(f"成功将 {pdf_file} 转换为 {docx_file}")
    except FileNotFoundError:
        print(f"错误：未找到文件 {pdf_file}")
    except Exception as e:
        print(f"发生未知错误: {e}")


if __name__ == "__main__":
    pdf_file = '/home/disky/lab/股权激励计划/3.pdf'
    docx_file = '/home/disky/lab/股权激励计划/3.docx'
    convert_pdf_to_docx(pdf_file, docx_file)
    