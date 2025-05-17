package com.pdfxplorer.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class PythonPdfRenderer {
    private final String pythonScript;
    private final ObjectMapper objectMapper;
    private Process pythonProcess;
    private String currentPdfPath;

    public PythonPdfRenderer() {
        this.objectMapper = new ObjectMapper();
        // Get the absolute path to the Python script and interpreter
        String userDir = System.getProperty("user.dir");
        this.pythonScript = Paths.get(userDir, "src", "main", "python", "pdf_renderer.py")
                .toAbsolutePath().toString();

        // Verify Python script exists
        if (!java.nio.file.Files.exists(Paths.get(pythonScript))) {
            throw new RuntimeException("Python script not found at: " + pythonScript);
        }
    }

    private String getPythonInterpreter() {
        String userDir = System.getProperty("user.dir");
        String venvPython = Paths.get(userDir, "venv", "bin", "python3")
                .toAbsolutePath().toString();

        if (java.nio.file.Files.exists(Paths.get(venvPython))) {
            return venvPython;
        }
        return "python3"; // Fallback to system Python
    }

    public CompletableFuture<Image> renderPage(String pdfPath, int pageNumber, double zoom) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build the command with python3 from virtual environment
                ProcessBuilder processBuilder = new ProcessBuilder(
                        getPythonInterpreter(),
                        pythonScript,
                        "render",
                        pdfPath,
                        String.valueOf(pageNumber),
                        String.valueOf(zoom));

                // Set up process environment
                processBuilder.redirectErrorStream(true);

                // Start the process
                Process process = processBuilder.start();

                // Read the output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                        System.out.println("Python output: " + line); // Debug output
                    }
                }

                // Wait for the process to complete
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException(
                            "Python process failed with exit code: " + exitCode + "\nOutput: " + output);
                }

                // Parse the JSON response
                JsonNode response = objectMapper.readTree(output.toString());

                if (!response.get("success").asBoolean()) {
                    throw new RuntimeException("Python renderer error: " +
                            response.get("error").asText());
                }

                // Convert base64 image to JavaFX Image
                String base64Image = response.get("image").asText();
                byte[] imageData = Base64.getDecoder().decode(base64Image);
                return new Image(new ByteArrayInputStream(imageData));

            } catch (Exception e) {
                throw new RuntimeException("Failed to render PDF page: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<JsonNode> getDocumentInfo(String pdfPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        getPythonInterpreter(),
                        pythonScript,
                        "info",
                        pdfPath);

                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                        System.out.println("Python output: " + line); // Debug output
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException(
                            "Python process failed with exit code: " + exitCode + "\nOutput: " + output);
                }

                JsonNode response = objectMapper.readTree(output.toString());

                if (!response.get("success").asBoolean()) {
                    throw new RuntimeException("Python renderer error: " +
                            response.get("error").asText());
                }

                return response.get("info");

            } catch (Exception e) {
                throw new RuntimeException("Failed to get document info: " + e.getMessage(), e);
            }
        });
    }
}