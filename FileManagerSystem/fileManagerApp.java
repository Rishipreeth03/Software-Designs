package FileManagerSystem;
import java.io.*;
import java.nio.file.*;
import java.util.*;

class FileManager{
    private VersionControlManager vcManager=new VersionControlManager();
    public void createFile(String path,String content) throws IOException{
        Files.write(Paths.get(path), content.getBytes(), StandardOpenOption.CREATE);
        vcManager.saveVersion(path, content);
    }
    public String readFile(String path)throws IOException{
        return new String(Files.readAllBytes(Paths.get(path)));
    }
    public void updateFile(String path,String newContent) throws IOException{
        Files.write(Paths.get(path),newContent.getBytes(),StandardOpenOption.TRUNCATE_EXISTING);
        vcManager.saveVersion(path, newContent);
    }
    public void deleteFile(String path) throws IOException{
        Files.deleteIfExists(Paths.get(path));
    }
    public List<String> listFiles(String dir) throws IOException{
        List<String> result=new ArrayList<>();
        Files.list(Paths.get(dir)).forEach(p->result.add(p.getFileName().toString()));
        return result;
    }
    public List<String> getVersions(String path) {
        return vcManager.getVersions(path);
    }

    public void rollback(String path, int versionIndex) throws IOException {
        vcManager.rollBack(path, versionIndex);
    }
}

class BatchOperationManager{
    private List<Runnable> operations =new ArrayList<>();
    public void addOperation(Runnable op){
        operations.add(op);
    }
    public void executeAll(){
        for(Runnable op:operations){
            op.run();
        }
        operations.clear();
    }
}

class LinkManager{
    public void createSoftLink(String target,String linkPath) throws IOException{
        Files.createSymbolicLink(Paths.get(linkPath),Paths.get(target));
    }
    public void createHardLink(String target,String linkPath) throws IOException{
        Files.createLink(Paths.get(linkPath),Paths.get(target));
    }
}

class VersionControlManager{
    private Map<String,List<String>> versions=new HashMap<>();
    public void saveVersion(String filePath,String content){
        versions.putIfAbsent(filePath, new ArrayList<>());
        versions.get(filePath).add(content);
    }
    public List<String> getVersions(String filePath){
        return versions.getOrDefault(filePath, new ArrayList<>());
    }
    public void rollBack(String filePath,int versionIndex) throws IOException{
        List<String> fileVersions=versions.get(filePath);
        if(fileVersions!=null && versionIndex<fileVersions.size()){
            String content=fileVersions.get(versionIndex);
            Files.write(Paths.get(filePath),content.getBytes(),StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}

public class fileManagerApp{
    public static void main(String[] args) throws IOException {
        try{
            FileManager fm=new FileManager();
            fm.createFile("test.txt","Hello World");
            System.out.println("File Content"+fm.readFile("test.txt"));

            fm.updateFile("test.txt", "Updated Content");
            System.out.println("Updated :"+fm.readFile("test.txt"));

            System.out.println("Versions: " + fm.getVersions("test.txt"));
            fm.rollback("test.txt", 0);

            System.out.println("Rolled Back :"+fm.readFile("test.txt"));

            System.out.println("Files in dir :"+fm.listFiles("."));
            //Batch operations
            BatchOperationManager batch = new BatchOperationManager();
            batch.addOperation(() -> {
                try { fm.createFile("f1.txt", "File1"); } catch (Exception e) {}
            });
            batch.addOperation(() -> {
                try { fm.createFile("f2.txt", "File2"); } catch (Exception e) {}
            });
            batch.executeAll();

            //Soft link
            LinkManager lm = new LinkManager();
            try {
                lm.createSoftLink("test.txt", "softlink.txt");
            } catch (IOException | UnsupportedOperationException e) {
                System.out.println("Skipping soft link (need admin privileges on Windows).");
            }

            lm.createHardLink("test.txt", "hardlink.txt");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}