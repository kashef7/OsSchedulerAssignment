import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

abstract class Scheduler {
    List<Process> processes = new ArrayList<>();
    Queue<String> ExcutionOrder = new LinkedList<>();
    abstract void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching);

}
