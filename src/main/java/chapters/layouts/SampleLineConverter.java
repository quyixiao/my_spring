package chapters.layouts;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SampleLineConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        StackTraceElement[] cda = event.getCallerData();
        if (cda != null && cda.length > 0) {
            String a = cda[0].getClassName();
            if(a.endsWith("LoggerUtils")){
                return Integer.toString(cda[1].getLineNumber());
            }
            return Integer.toString(cda[0].getLineNumber());
        } else {
            return CallerData.NA;
        }



    }


} 