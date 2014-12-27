package com.cardiomood.android.fragments.details;

import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;
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
                    "Intervals count:\t%3$d with %33$d artifacts present (%34$d%%)\n" +
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
                    "TP  =\t%10$f\t\t\t\tln TP  = %17$f \n" +
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
                    "WN1   =\t%31$f\n" +
                    "WN4   =\t%35$f\n" +
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

//    public static final String HTML_TABLE_REPORT_FORMAT
//            = "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" +
//            "\t\t\t<tbody>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td width=\"100\">\n" +
//            "\t\t\t\t\t\trrs number</td>\n" +
//            "\t\t\t\t\t<td width=\"100\">\n" +
//            "\t\t\t\t\t\t<strong>333</strong></td>\n" +
//            "\t\t\t\t\t<td width=\"100\">\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td width=\"100\">\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tartif. count</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>333</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tmRR</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>333</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tLF/HF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>23323223</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tSDNN</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tLF norm</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tRMSSD</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>17.58</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tHF norm</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tpNN50</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>1.44</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tVLF/HF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tIC</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tTP</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tln TP</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tMo</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tVLF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tAmo</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tVLF%</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tSI</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tln VLF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tLF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tMxDMn</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tLF%</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tWN5</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tln LF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tWN4</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tHF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tWN1</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tHF%</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tHRVTi</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t\t<tr>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\tln HF</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t<strong>-</strong></td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t\t<td>\n" +
//            "\t\t\t\t\t\t&nbsp;</td>\n" +
//            "\t\t\t\t</tr>\n" +
//            "\t\t\t</tbody>\n" +
//            "\t\t</table>";

    private static final ArtifactFilter FILTER = new PisarukArtifactFilter();

    private String reportFormat = DEFAULT_REPORT_FORMAT;
    private Date startDate = new Date();
    private Date endDate;
    private String tag;
    private double[] rrIntervals = new double[0];
    private int artifactsCount = 0;

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
                getHistogram128().getWN1(),
                getHistogram128().getHRVTi(),
                artifactsCount,
                Math.round(artifactsCount*100.0f/rrIntervals.length),
                getHistogram128().getWN4()
        );
    }

    public static class Builder {

        private TextReport report;
        private int filterCount;

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
            if (report.endDate == null)
                report.endDate = new Date(report.startDate.getTime() + Math.round(HeartRateUtils.getSum(report.rrIntervals)));

            for (int i=0; i<filterCount; i++) {
                report.rrIntervals = FILTER.doFilter(report.rrIntervals);
            }
            report.artifactsCount = FILTER.getArtifactsCount(report.rrIntervals);
            report.spectrum = new SpectralAnalysis(report.rrIntervals);
            report.histogram50 = new Histogram(report.rrIntervals, 50);
            report.mRR = HeartRateUtils.getMRR(report.rrIntervals);
            report.heartRate = 60*1000 / report.mRR;
            report.SDNN = HeartRateUtils.getSDNN(report.rrIntervals);
            report.RMSSD = HeartRateUtils.getRMSSD(report.rrIntervals);
            report.pNN50 = HeartRateUtils.getPNN50(report.rrIntervals);
            report.histogram128 = new Histogram128Ext(report.rrIntervals);

            return report;
        }

        public void setFilterCount(int filterCount) {
            this.filterCount = filterCount;
        }

        public int getFilterCount() {
            return filterCount;
        }
    }
}
