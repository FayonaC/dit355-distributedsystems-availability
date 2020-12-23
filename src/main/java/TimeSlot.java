import java.time.LocalTime;

public class TimeSlot {
    LocalTime startTime;
    LocalTime endTime;
    boolean available;

    public TimeSlot(LocalTime startTime, LocalTime endTime, boolean available) {
        setEndTime(endTime);
        setStartTime(startTime);
        setAvailable(available);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "{ \"startTime\": \"" + startTime +
                "\", \"endTime\": \"" + endTime +
                "\" }";
    }
}
