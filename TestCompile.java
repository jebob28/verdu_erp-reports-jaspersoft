
import net.sf.jasperreports.engine.JasperCompileManager;
import java.io.FileInputStream;

public class TestCompile {
    public static void main(String[] args) {
        try {
            System.out.println("Attempting to compile Relatoriodecatacao.jrxml...");
            JasperCompileManager.compileReportToFile("debug_Relatoriodecatacao.jrxml", "Relatoriodecatacao.jasper");
            System.out.println("Compilation successful!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
