import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.regex.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.*;


public class Main {



    public static void main(String[] args) throws Exception {
        System.out.println("Choose scheduler to test:");
        System.out.println("1) SJF");
        System.out.println("2) RR");
        System.out.println("3) Priority");
        System.out.println("4) AG");
        System.out.print("Enter choice (1-4): ");

        Scanner sc = new Scanner(System.in);
        int choice = 1;
        try {
            choice = Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            choice = 1;
        }

        String schedulerKey;
        switch (choice) {
            case 1: schedulerKey = "SJF"; break;
            case 2: schedulerKey = "RR"; break;
            case 3: schedulerKey = "Priority"; break;
            case 4: schedulerKey = "AG"; break;
            default: schedulerKey = "SJF"; break;
        }

        String testsDir;
        if (schedulerKey.equals("AG")) {
            testsDir = "test_cases_v5/AG";// change to test_cases_v5/AG when running from windows
        } else {
            testsDir = "test_cases_v5/Other_Schedulers";//change to test_cases_v5/Other_Schedulers when running from windows
        }

        List<Path> files = Files.list(Paths.get(testsDir))
            .filter(p -> p.getFileName().toString().endsWith(".json"))
            .sorted()
            .collect(Collectors.toList());

        if (files.isEmpty()) {
            System.err.println("No test files found in " + testsDir);
            return;
        }

        int passed = 0, total = 0;
        for (Path file : files) {
            total++;
            System.out.println("\n--- Running test: " + file.getFileName() + " ---");
            String json = readFile(file);

            // read rrQuantum/contextSwitch if present
            Integer rrQuantum = matchIntField(json, "rrQuantum");
            Integer contextSwitch = matchIntField(json, "contextSwitch");
            Integer agingInterval = matchIntField(json, "agingInterval");

            // parse processes
            List<Map<String,String>> procDefs = parseProcesses(json);

            // create scheduler instance and populate processes
            Object scheduler = createSchedulerInstance(schedulerKey, agingInterval == null ? 0 : agingInterval);
            if (scheduler == null) {
                System.err.println("Unsupported scheduler: " + schedulerKey);
                continue;
            }

            // create Process objects and add to scheduler.processes
            // we assume Process has constructor Process(arrival, burst, name) and setters for priority/quantum
            List<Object> procObjects = new ArrayList<>();
            for (Map<String,String> pd : procDefs) {
                int arrival = Integer.parseInt(pd.get("arrival"));
                int burst = Integer.parseInt(pd.get("burst"));
                String name = pd.get("name");
                Process p = new Process(arrival, burst, name);
                if (pd.containsKey("priority")) p.setPriority(Integer.parseInt(pd.get("priority")));
                if (pd.containsKey("quantum")) p.setQuantum(Integer.parseInt(pd.get("quantum")));
                // add to scheduler.processes list (accessible because classes are in same package)
                ((Scheduler) scheduler).processes.add(p);
                procObjects.add(p);
            }

            int noOfProcesses = procObjects.size();
            int rr = rrQuantum == null ? 0 : rrQuantum;
            int cs = contextSwitch == null ? 0 : contextSwitch;

            // run scheduler while capturing printed output (so we can extract executionOrder
            // from schedulers that print it instead of populating ExcutionOrder)
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            try {
                System.setOut(ps);
                ((Scheduler) scheduler).schedule(noOfProcesses, rr, cs);
            } finally {
                System.out.flush();
                System.setOut(oldOut);
            }
            String capturedOutput = baos.toString();

            // collect actual results
            List<String> actualExecOrder = new ArrayList<>();
            // first, try to read from the scheduler's ExcutionOrder (preferred)
            ((Queue<String>) ((Scheduler) scheduler).ExcutionOrder).forEach(actualExecOrder::add);

            // if empty, try to parse the printed executionOrder from captured output
            if (actualExecOrder.isEmpty()) {
                Pattern p = Pattern.compile("executionOrder\\s*:\\s*\\[([^\\]]*)\\]");
                Matcher m = p.matcher(capturedOutput);
                if (m.find()) {
                    String inner = m.group(1).trim();
                    if (!inner.isEmpty()) {
                        String[] parts = inner.split(",");
                        for (String part : parts) {
                            String token = part.trim();
                            if (!token.isEmpty()) actualExecOrder.add(token);
                        }
                    }
                }
            }

            Map<String, Double> actualWait = new HashMap<>();
            Map<String, Double> actualTurn = new HashMap<>();
            List<Process> processesRef = ((Scheduler) scheduler).processes;
            for (Process p : processesRef) {
                actualWait.put(p.getName(), (double) p.getWaitingTime());
                actualTurn.put(p.getName(), (double) p.getTurnAroundTime());
            }
            double actualAvgWait = processesRef.stream().mapToDouble(Process::getWaitingTime).average().orElse(0.0);
            double actualAvgTurn = processesRef.stream().mapToDouble(Process::getTurnAroundTime).average().orElse(0.0);
            if ("AG".equals(schedulerKey)) {
            System.out.println("\"processResults\": [");
            for (int i = 0; i < processesRef.size(); i++) {
                Process p = processesRef.get(i);
                // print quantumHistory as a JSON array
                String qh = p.getQuantumHistory().toString();
                System.out.print("  {\"name\": \"" + p.getName() + "\", \"waitingTime\": " + p.getWaitingTime()
                        + ", \"turnaroundTime\": " + p.getTurnAroundTime()
                        + ", \"quantumHistory\": " + qh + "}");
                if (i < processesRef.size() - 1) System.out.println(",");
                else System.out.println();
            }
            System.out.println("],");
            System.out.println("\"averageWaitingTime\": " + String.format("%.2f", actualAvgWait) + ",");
            System.out.println("\"averageTurnaroundTime\": " + String.format("%.2f", actualAvgTurn));
        }
            // parse expected for this scheduler
            Map<String, Object> expected = parseExpectedForScheduler(json, schedulerKey);

            // Now compare
        // Now compare
                boolean ok = true;

                // --- Always print execution orders (expected from JSON and actual from scheduler)
                List<String> expectedExecOrder = (List<String>) expected.get("executionOrder");
                System.out.println("Expected execution order: " + expectedExecOrder);
                System.out.println("Scheduler execution order: " + actualExecOrder);

                if (expectedExecOrder != null) {
                    if (!expectedExecOrder.equals(actualExecOrder)) {
                        ok = false;
                        System.out.println("Execution order mismatch.");
                    } else {
                        System.out.println("Execution order: OK");
                    }
                } else {
                    System.out.println("No expected execution order in file for this scheduler.");
                }

                // per-process waiting/turnaround compare (same as before)
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> expectedProcResults = (List<Map<String,Object>>) expected.get("processResults");
                if (expectedProcResults != null) {
                    for (Map<String,Object> er : expectedProcResults) {
                        String name = (String) er.get("name");
                        double expW = ((Number) er.get("waitingTime")).doubleValue();
                        double expT = ((Number) er.get("turnaroundTime")).doubleValue();
                        Double aW = actualWait.get(name);
                        Double aT = actualTurn.get(name);
                        if (aW == null || aT == null) {
                            ok = false;
                            System.out.println("Missing actual results for process " + name);
                            continue;
                        }
                        if (Math.abs(expW - aW) > TOLERANCE) {
                            ok = false;
                            System.out.printf("Process %s waitingTime mismatch: expected=%.2f actual=%.2f%n", name, expW, aW);
                        } else {
                            System.out.printf("Process %s waitingTime: OK (expected=%.2f actual=%.2f)%n", name, expW, aW);
                        }
                        if (Math.abs(expT - aT) > TOLERANCE) {
                            ok = false;
                            System.out.printf("Process %s turnaroundTime mismatch: expected=%.2f actual=%.2f%n", name, expT, aT);
                        } else {
                            System.out.printf("Process %s turnaroundTime: OK (expected=%.2f actual=%.2f)%n", name, expT, aT);
                        }
                    }
                } else {
                    System.out.println(".");
                }

                // --- Averages: always print expected vs actual and show OK/MISMATCH where an expected value exists
               Double expectedAvgW = (Double) expected.get("averageWaitingTime");
                Double expectedAvgT = (Double) expected.get("averageTurnaroundTime");

                String expWStr = expectedAvgW == null ? "N/A" : String.format("%.2f", expectedAvgW);
                String expTStr = expectedAvgT == null ? "N/A" : String.format("%.2f", expectedAvgT);
                String actWStr = String.format("%.2f", actualAvgWait);
                String actTStr = String.format("%.2f", actualAvgTurn);

                if (expectedAvgW != null) {
                    boolean okW = Math.abs(expectedAvgW - actualAvgWait) <= TOLERANCE;
                    System.out.printf("Expected average waiting: %s; Actual average waiting: %s; %s%n",
                            expWStr, actWStr, okW ? "OK" : "MISMATCH");
                    if (!okW) ok = false;
                } else {
                    System.out.printf("Expected average waiting: %s; Actual average waiting: %s%n", expWStr, actWStr);
                }

                if (expectedAvgT != null) {
                    boolean okT = Math.abs(expectedAvgT - actualAvgTurn) <= TOLERANCE;
                    System.out.printf("Expected average turnaround: %s; Actual average turnaround: %s; %s%n",
                            expTStr, actTStr, okT ? "OK" : "MISMATCH");
                    if (!okT) ok = false;
                } else {
                    System.out.printf("Expected average turnaround: %s; Actual average turnaround: %s%n", expTStr, actTStr);
                }
                if (ok) {
                    passed++;
                    System.out.println("TEST PASSED");
                } else {
                    System.out.println("TEST FAILED");
                }
            // reset scheduler.processes and ExcutionOrder for next test
            ((Scheduler) scheduler).processes.clear();
            ((Scheduler) scheduler).ExcutionOrder.clear();
        }

        System.out.printf("\nSummary: %d/%d tests passed for scheduler %s%n", passed, total, schedulerKey);
    }

    // ---------- helpers ----------

    private static String readFile(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    // parse the "processes" array into a list of maps with keys: name, arrival, burst, priority?, quantum?
    private static List<Map<String,String>> parseProcesses(String json) {
        List<Map<String,String>> procs = new ArrayList<>();
        int procIdx = json.indexOf("\"processes\"");
        if (procIdx < 0) return procs;
        int arrStart = json.indexOf('[', procIdx);
        int arrEnd = findMatchingBracket(json, arrStart);
        if (arrStart < 0 || arrEnd < 0) return procs;
        String procArray = json.substring(arrStart + 1, arrEnd);

        Pattern objPat = Pattern.compile("\\{([^}]*)\\}", Pattern.DOTALL);
        Matcher m = objPat.matcher(procArray);
        while (m.find()) {
            String obj = m.group(1);
            Map<String,String> pd = new HashMap<>();
            String name = matchStringField(obj, "name");
            if (name != null) pd.put("name", name);
            Integer arrival = matchIntFieldRaw(obj, "arrival");
            Integer burst = matchIntFieldRaw(obj, "burst");
            Integer priority = matchIntFieldRaw(obj, "priority");
            Integer quantum = matchIntFieldRaw(obj, "quantum");
            if (arrival != null) pd.put("arrival", String.valueOf(arrival));
            if (burst != null) pd.put("burst", String.valueOf(burst));
            if (priority != null) pd.put("priority", String.valueOf(priority));
            if (quantum != null) pd.put("quantum", String.valueOf(quantum));
            procs.add(pd);
        }
        return procs;
    }
   

    // returns matching bracket index for '[' or '{'
    private static int findMatchingBracket(String s, int openIdx) {
        if (openIdx < 0 || openIdx >= s.length()) return -1;
        char open = s.charAt(openIdx);
        char close = (open == '[') ? ']' : '}';
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String matchStringField(String src, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : null;
    }
    private static Integer matchIntFieldRaw(String src, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(src);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    // find integer fields anywhere in json (used for rrQuantum/contextSwitch at top level)
    private static Integer matchIntField(String src, String key) {
        return matchIntFieldRaw(src, key);
    }

    // create scheduler instance given key (SJF, RR, Priority, AG)
    private static Object createSchedulerInstance(String key, int agingInterval) {
        switch (key) {
            case "SJF": return new PreemptiveSJF();
            case "RR": return new RoundRobin();
            case "Priority": return new PreemptivePriority(agingInterval);
            case "AG": return new AgScheduler();
            default: return null;
        }
    }

private static Map<String,Object> parseExpectedForScheduler(String json, String schedulerKey) {
    Map<String,Object> out = new HashMap<>();
    int expIdx = json.indexOf("\"expectedOutput\"");
    if (expIdx < 0) return out;

    // find the full expectedOutput object
    int expStart = json.indexOf('{', expIdx);
    int expEnd = findMatchingBracket(json, expStart);
    String expBlock = expStart >= 0 && expEnd > expStart ? json.substring(expStart + 1, expEnd) : "";

    String targetBlock = "";

    if (schedulerKey.equals("AG")) {
        // AG uses the top-level expectedOutput block
        targetBlock = expBlock;
    } else {
        // Locate the named scheduler block (e.g. "SJF", "RR", "Priority") in the original json
        int keyIdx = json.indexOf("\"" + schedulerKey + "\"", expIdx);
        if (keyIdx >= 0) {
            int objStart = json.indexOf('{', keyIdx);
            if (objStart >= 0) {
                int objEnd = findMatchingBracket(json, objStart);
                if (objEnd > objStart) {
                    targetBlock = json.substring(objStart + 1, objEnd);
                }
            }
        }
    }

    // executionOrder (if present)
    List<String> exec = parseStringArrayField(targetBlock, "executionOrder");
    out.put("executionOrder", exec);

    // processResults array
    List<Map<String,Object>> pres = new ArrayList<>();
    Pattern prPat = Pattern.compile("\"processResults\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL);
    Matcher prM = prPat.matcher("{" + targetBlock + "}");
    if (prM.find()) {
        String prArray = prM.group(1);
        Pattern objPat = Pattern.compile("\\{([^}]*)\\}", Pattern.DOTALL);
        Matcher m2 = objPat.matcher(prArray);
        while (m2.find()) {
            String o = m2.group(1);
            String name = matchStringField(o, "name");
            Integer waiting = matchIntFieldRaw(o, "waitingTime");
            Integer turn = matchIntFieldRaw(o, "turnaroundTime");
            Map<String,Object> map = new HashMap<>();
            if (name != null) map.put("name", name);
            if (waiting != null) map.put("waitingTime", waiting.doubleValue());
            if (turn != null) map.put("turnaroundTime", turn.doubleValue());
            pres.add(map);
        }
    }
    out.put("processResults", pres);

    // averages (now read from the correctly extracted targetBlock)
    Double avgW = matchDoubleField(targetBlock, "averageWaitingTime");
    Double avgT = matchDoubleField(targetBlock, "averageTurnaroundTime");
    out.put("averageWaitingTime", avgW);
    out.put("averageTurnaroundTime", avgT);

    return out;
}
    private static List<String> parseStringArrayField(String block, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL);
        Matcher m = p.matcher(block);
        if (!m.find()) return null;
        String arr = m.group(1);
        List<String> out = new ArrayList<>();
        Pattern s = Pattern.compile("\"([^\"]+)\"");
        Matcher ms = s.matcher(arr);
        while (ms.find()) out.add(ms.group(1));
        return out;
    }
        
    private static final double TOLERANCE = 0.21;
    private static Double matchDoubleField(String src, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(src);
        if (!m.find()) return null;
        try {
            return Double.valueOf(m.group(1));
        } catch (Exception e) {
            return null;
        }
    }
     
}
