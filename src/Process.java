//The class of the process
class Process {
    private int arrivalTime;
    private int burstTime;
    private int waitingTime;
    private String name;

    public Process(int arriveTime, int burstTime, String name) {
        this.arrivalTime = arriveTime;
        this.burstTime = burstTime;
        this.name = name;
        waitingTime = 0;
    }

    // Get the arrive time value
    public int getArrivalTime() {
        return arrivalTime;
    }

    // Set the arrive time value
    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    // Get the burst time value
    public int getBurstTime() {
        return burstTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    // Set the burst time value
    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
    }

    // Get the process name
    public String getName() {

        return name;
    }
}