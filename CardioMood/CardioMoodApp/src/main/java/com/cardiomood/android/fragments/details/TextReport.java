package com.cardiomood.android.fragments.details;

import com.cardiomood.math.HeartRateMath;
import com.cardiomood.math.histogram.Histogram;
import com.cardiomood.math.histogram.Histogram128Ext;
import com.cardiomood.math.spectrum.SpectralAnalysis;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by danon on 17.03.14.
 */
public class TextReport {

    public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    public static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);

    public static final String DEFAULT_REPORT_FORMAT =
            "CardioMood Report\n" +
                    "\n" +
                    "Date: \t\t\t%1$s\n" +
                    "Measurement time:\t%2$s\n" +
                    "Intervals count:\t%3$d\n" +
                    "Tag: %4$s\n" +
                    "\n" +
                    "\n" +
                    "Results:  \n" +
                    "\n" +
                    "Time series parameters:\n" +
                    "HR    =\t%5$d\n" +
                    "mRR   = %6$d\n" +
                    "SDNN  = %7$f\n" +
                    "RMSSD = %8$f\n" +
                    "pNN50 = %9$f\n" +
                    "\n" +
                    "\n" +
                    "Frequency analysis:\n" +
                    "TP  =\t%10$f\t\t\tln TP  = %17$f \n" +
                    "VLF =\t%11$f\tVLF%% =\t%14$f\tln VLF = %18$f\n" +
                    "LF  =\t%12$f\tLF%%  =\t%15$f\tln LF  = %19$f\n" +
                    "HF  =\t%13$f\tHF%%  =\t%16$f\tln HF  = %20$f\n" +
                    "\n" +
                    "Spectral indicies:\n" +
                    "LF/HF   =\t\t\t%21$f\n" +
                    "LF norm = LF/(LF+HF)*100%% =\t%22$f\n" +
                    "HF norm = HF/(LF+HF)*100%% =\t%23$f\n" +
                    "VLF/HF  =\t\t\t%24$f\t\t\t \n" +
                    "IC      = (VLF+LF)/HF =\t\t%25$f\n" +
                    "\n" +
                    "Histogram (50ms step):\n" +
                    "Mo   =\t%26$f\n" +
                    "AMo%% =\t%27$f\n" +
                    "SI   =\t%28$f\n" +
                    "\n" +
                    "Variation range (Var) =\t%29$f\n" +
                    "WN5   =\t%30$f\n" +
                    "WN10  = %31$f\n" +
                    "WAM5  =\tN/A\n" +
                    "WAM10 = N/A\n" +
                    "\n" +
                    "HRVTi =\t%32$f (step 1/128 s)\n" +
                    "\n" +
                    "Scatterogram parameters:\n" +
                    "L   =\tN/A\n" +
                    "W   =\tN/A\n" +
                    "L/W =\tN/A\n" +
                    "S   =\tN/A\n";

    private String reportFormat = DEFAULT_REPORT_FORMAT;
    private Date startDate;
    private Date endDate;
    private String tag;
    private double[] rrIntervals;

    private double heartRate;
    private double mRR;
    private double SDNN;
    private double RMSSD;
    private double pNN50;

    private SpectralAnalysis spectrum;
    private Histogram histogram50;
    private Histogram128Ext histogram128;

    private TextReport() {
        // use Builder to create an instance
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public double[] getRrIntervals() {
        return rrIntervals;
    }

    public double getHeartRate() {
        return heartRate;
    }

    public double getmRR() {
        return mRR;
    }

    public double getSDNN() {
        return SDNN;
    }

    public double getRMSSD() {
        return RMSSD;
    }

    public double getpNN50() {
        return pNN50;
    }

    public SpectralAnalysis getSpectrum() {
        return spectrum;
    }

    public Histogram getHistogram50() {
        return histogram50;
    }

    public Histogram128Ext getHistogram128() {
        return histogram128;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return String.format(
                getReportFormat(),
                DATE_FORMAT.format(getStartDate()),
                TIME_FORMAT.format(getStartDate()) + " - " + TIME_FORMAT.format(getEndDate()),
                getRrIntervals().length,
                getTag(),
                Math.round(getHeartRate()),
                Math.round(getmRR()),
                getSDNN(),
                getRMSSD(),
                getpNN50(),
                getSpectrum().getTP(),
                getSpectrum().getVLF(),
                getSpectrum().getLF(),
                getSpectrum().getHF(),
                getSpectrum().getVLF()*100/(getSpectrum().getTP()-getSpectrum().getULF()),
                getSpectrum().getLF()*100/(getSpectrum().getTP()-getSpectrum().getULF()),
                getSpectrum().getHF()*100/(getSpectrum().getTP()-getSpectrum().getULF()),
                Math.log(getSpectrum().getTP()),
                Math.log(getSpectrum().getVLF()),
                Math.log(getSpectrum().getLF()),
                Math.log(getSpectrum().getHF()),
                getSpectrum().getLF()/getSpectrum().getHF(),
                getSpectrum().getLF()*100/(getSpectrum().getHF()+getSpectrum().getLF()),
                getSpectrum().getHF()*100/(getSpectrum().getHF()+getSpectrum().getLF()),
                getSpectrum().getVLF()/getSpectrum().getHF(),
                (getSpectrum().getVLF() + getSpectrum().getLF())/getSpectrum().getHF(),
                getHistogram50().getMo(),
                getHistogram50().getAMo(),
                getHistogram50().getSI(),
                getHistogram50().getMxDMn(),
                getHistogram128().getWN5(),
                getHistogram128().getWN10(),
                getHistogram128().getHRVTi()
        );
    }

    public static class Builder {

        private TextReport report;

        public Builder() {
            report = new TextReport();
        }

        public Builder setReportFormat(String reportFormat) {
            report.reportFormat = reportFormat;
            return this;
        }

        public Builder setStartDate(Date startDate) {
            report.startDate = startDate;
            return this;
        }

        public Builder setEndDate(Date endDate) {
            report.endDate = endDate;
            return this;
        }

        public Builder setTag(String tag) {
            report.tag = tag;
            return this;
        }

        public Builder setRRIntervals(double[] rrIntervals) {
            report.rrIntervals = rrIntervals;
            return this;
        }

        public TextReport build() {
            HeartRateMath math = new HeartRateMath(report.rrIntervals);

            if (report.endDate == null)
                report.endDate = new Date(report.startDate.getTime() + Math.round(math.getDuration()));

            report.spectrum = new SpectralAnalysis(math.getTime(), math.getRrIntervals());
            report.histogram50 = new Histogram(report.rrIntervals, 50);
            report.mRR = math.getMean();
            report.heartRate = 60*1000 / report.mRR;
            report.SDNN = math.getSDNN();
            report.RMSSD = math.getRMSSD();
            report.pNN50 = math.getPNN50();
            report.histogram128 = new Histogram128Ext(report.rrIntervals);

            return report;
        }
    }
}
