import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Process> processes = new ArrayList<>();

        // Loading values from your test case
        Process p1 = new Process(0, 17, "P1");
        p1.setPriority(4); p1.setQuantum(7);

        Process p2 = new Process(2, 6, "P2");
        p2.setPriority(7); p2.setQuantum(9);

        Process p3 = new Process(5, 11, "P3");
        p3.setPriority(3); p3.setQuantum(4);

        Process p4 = new Process(15, 4, "P4");
        p4.setPriority(6); p4.setQuantum(6);

        processes.add(p1);
        processes.add(p2);
        processes.add(p3);
        processes.add(p4);

        AgScheduler scheduler = new AgScheduler();
        scheduler.schedule(processes);
    }
}