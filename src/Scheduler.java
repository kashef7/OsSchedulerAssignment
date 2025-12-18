import java.util.ArrayList;
import java.util.List;

abstract class Scheduler {
    List<Process> processes = new ArrayList<>();
    abstract void schedule(int noOfProcesses, int roundRobinTimeQuantum, int ContextSwitching);

}
