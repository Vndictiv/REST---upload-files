package pl.borowik.storageApp.localStorage.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.borowik.storageApp.localStorage.model.LocalFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api/local/files")
public class LocalStorageController {

    private static final Logger LOGGER = Logger.getLogger(LocalStorageController.class.getName());

    private ServletContext servletContext; //dla ścieżki plików - ścieżka tymaczasowa
    private String uploads; //ścieżka

    public LocalStorageController(ServletContext servletContext) {
        this.servletContext = servletContext;
        createContextDirectory();
    }

    private void createContextDirectory() {
        uploads = servletContext.getRealPath("/uploads/");
        LOGGER.log(Level.INFO, uploads);
        Path path = Paths.get(uploads);
       
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/files")
    public List<LocalFile> getResources() throws IOException {

        return Files.walk(Paths.get(uploads))
                .filter(Files::isRegularFile)
                .map(f -> {

                    try {
                        BasicFileAttributes bs = Files.readAttributes(f.toAbsolutePath(), BasicFileAttributes.class);

                        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/files/download/")
                                .path(f.getFileName().toString())
                                .toUriString();

                        String fileDeleteUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/files/delete/")
                                .path(f.getFileName().toString())
                                .toUriString();

                        return new LocalFile(
                                f.getFileName().toString(),
                                bs.creationTime().toString(),
                                bs.lastModifiedTime().toString(),
                                bs.size(),
                                fileDownloadUri,
                                fileDeleteUri,
                                Files.probeContentType(f.toAbsolutePath()));

                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());
    }

    @PostMapping("/files")
    public void upload(@RequestParam("file") MultipartFile file) throws IOException {

        Path path = Paths.get(uploads + file.getOriginalFilename());
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

    }


    @GetMapping(value = "/files/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename, HttpServletResponse response) throws IOException {
        //void*

        Path path = Paths.get(uploads + filename);
        Resource resource = new UrlResource(path.toUri());
        // byte[] content = Files.readAllBytes(path);

        //   MultipartFile targetFile = new MockMultipartFile(fileName,
        //           fileName, "text/plain", content);

        File targetFile = new File(uploads + filename);
        //  InputStreamResource resource = new InputStreamResource(new FileInputStream(targetFile));
        //  MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

        String contentType = Files.probeContentType(path);
//        System.out.println(contentType);


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType)) //default: octet stream
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + targetFile.getName() + "\"")
                //  .header("Access-Control-Allow-Origin", "*")
                //  .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT")
                .contentLength(targetFile.length())
                .body(resource);

        // ----------*
        //   response.setContentType(contentType);
        //   response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        //   response.setStatus(HttpServletResponse.SC_OK);

        //   InputStream is = new FileInputStream(uploads + filename);
        //  FileCopyUtils.copy(is, response.getOutputStream());
        //  response.flushBuffer();

    }

    @DeleteMapping("/files/delete/{file}")
    public void delete(@PathVariable("file") String fileName) {

        File file = new File(uploads + fileName);
        if (file.exists()) {
            file.delete();
        }
    }

}
