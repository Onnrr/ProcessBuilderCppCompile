import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A wrapper class for compiling and running C++ programs with ProcessBuilder
 * Uses the g++ compiler installed in the machine, so it is not a compiler
 * but an abstraction for commands to run during compiling and running
 * --------------------------------------------------
 * At most one process can run concurrently therefore new instances must be created
 * if more than one program run
 * --------------------------------------------------
 * readFinished ensures that the reading of the process is completed before
 * making the reader null when process finishes
 */
public class CppComp {
    public Process activeProcess;
    BufferedWriter writer;
    BufferedReader reader;
    CompletableFuture<Boolean> readFinished;
    CompletableFuture<Boolean> compileSuccessful;

    /**
     * Creates instance of the class with no active process
     * Checks if the g++ compiler is installed in the machine
     * @throws Exception if the compiler is not installed cannot create instance
     */
    public CppComp() throws Exception {
        if (!checkCompiler()) {
            throw new Exception("g++ is not installed in the computer");
        }
        activeProcess = null;
        writer = null;
        reader = null;
        readFinished = null;
        compileSuccessful = null;
    }

    /**
     * Compiles and executes the C++ program given as a string
     * First creates and writes the code in the "program.cpp" file and
     * executes the file with executeFile method
     * @param cppCode C++ progra to run
     */
    public void executeCode(String cppCode) {
        createFile(cppCode, "program.cpp");
        File f = new File("program.cpp");
        try {
            executeFile(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void executeFile(String fileName) throws Exception {
        if (!fileName.endsWith(".cpp")) {
            throw new Exception("Given file must have extension \".cpp\"");
        }
        File f = new File(fileName);
        if (!f.isFile()) {
            throw new Exception("File \"" + fileName + "\" does not exist");
        }
        executeFile(f);
    }
    
    /**
     * Compiles and runs the C++ file given as argument
     * Creates two different processes (for compile and run)
     * Sets te active process to the one that runs the actual program
     * @param file C++ file
     * @throws Exception if the file does not have extension ".cpp"
     */
    public void executeFile(File file) throws Exception {
        if (!file.getName().endsWith(".cpp")) {
            throw new Exception("Given file must have extension \".cpp\"");
        }
        ProcessBuilder compilepb = new ProcessBuilder("g++",file.getName(), "-o", "prog");
        compilepb.redirectErrorStream(true);
        try {
            setActiveProcess(compilepb.start());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread waitCompile = new Thread(new CompileWait());
        Thread readCompile = new Thread(new AsyncRead());
        readCompile.start();
        waitCompile.start();
        Boolean success = compileSuccessful.get();
        if (!success) {
            System.out.println("Compile Failed");
            return;
        }
        
        
        ProcessBuilder pb = new ProcessBuilder("./prog");
        pb.redirectErrorStream(true);
        try {
            setActiveProcess(pb.start());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Thread wait = new Thread(new ProcessWait());
        Thread read = new Thread(new AsyncRead());
        read.start();
        wait.start();
    }

    /**
     * Writes the given string to the output stream of the active process
     * Also writes a new line character and flushes to directly execute
     * @param s input to be sent to the process
     * @return input sent
     * @throws Exception if there are no active processes
     */
    public boolean sendInput(String s) throws Exception {
        if (!hasRunningProcess()) {
            throw new Exception("There is no active process");
        }
        writer.write(s, 0, s.length());
        writer.newLine();
        writer.flush();
        return true;
    }


    public boolean hasRunningProcess() {
        return activeProcess != null;
    }

    /**
     * Sets the active process to given process
     * Crates instances of buffered reader and buffered writer for IO operations
     * Currently does not support change of active process while one is running
     * @param p process o set the active
     */
    private void setActiveProcess(Process p) {
        if (p == null) {
            activeProcess = null;
            writer = null;
            reader = null;
            readFinished = null;
        }
        else {
            activeProcess = p;
            writer = new BufferedWriter(new OutputStreamWriter(activeProcess.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            readFinished = new CompletableFuture<Boolean>();
            compileSuccessful = new CompletableFuture<Boolean>();
        }
    }


    /**
     * Checks if the g++ compiler is installed using another process
     * Runs "g++ --version"
     * @return true if the compiler is installed
     */
    public static boolean checkCompiler() {
        ProcessBuilder pb = new ProcessBuilder("g++", "--version");
        Process p;
        int status = -1;
        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            status = p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        if (status == 0) {
            return true;
        }
        return false;
        
    }

    private boolean createFile(String content, String fileName) {
        FileWriter myWriter;
        try {
            myWriter = new FileWriter(fileName);
            myWriter.write(content);
            myWriter.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Kills the active process
     * Sets the active process to null
     * Cannot use send input after this until a new process is active
     */
    public void killProcess() {
        activeProcess.destroy();
        setActiveProcess(null);
    }

    /**
     * Used for removing temporary files after the process is completed
     */
    private void removeFiles() {
        File f1 = new File("program.cpp");
        File f2 = new File("prog.exe");
        File f3 = new File("prog.out");
        File f4 = new File("prog");
        f1.delete();
        f2.delete();
        f3.delete();
        f4.delete();
    }

    /**
     * Waits for the active process to finish execution
     * Then waits for the process read if it has not yet been completed
     * Finally sets active process to null and removes the temporary files
     */
    private class ProcessWait implements Runnable {
        @Override
        public void run() {
            int status = -1;
            try {
                status = activeProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (status != 0) {
                System.err.println("Process failed");
                return;
            }
            try {
                Boolean finished = readFinished.get();
                if (finished) {
                    setActiveProcess(null);
                    removeFiles();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            
        }
    }

    /**
     * Waits for the active process to finish execution
     * Then waits for the process read if it has not yet been completed
     * Finally sets active process to null and removes the temporary files
     * Similar to ProcessWait but also sets the compile status for synchronization
     */
    private class CompileWait implements Runnable {
        @Override
        public void run() {
            int status = -1;
            try {
                status = activeProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (status != 0) {
                compileSuccessful.complete(false);
                return;
            }
            try {
                Boolean finished = readFinished.get();
                if (finished) {
                    setActiveProcess(null);
                    compileSuccessful.complete(true);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            
        }
    }

    /**
     * Asynchronously reads the input stream of the current process
     * and writes to the std.out
     */
    private class AsyncRead implements Runnable {
        @Override
        public void run() {
            int s;
            try {
                while (reader != null && ((s = reader.read()) != -1)) {
                    System.out.print((char)s);
                }
                if (readFinished != null) {
                    readFinished.complete(true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
