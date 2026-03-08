import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
public class PdfDump {
  public static void main(String[] args) throws Exception {
    PdfReader reader = new PdfReader(args[0]);
    for (int i = 1; i <= reader.getNumberOfPages(); i++) {
      System.out.println("--- PAGE " + i + " ---");
      System.out.println(PdfTextExtractor.getTextFromPage(reader, i));
    }
    reader.close();
  }
}
