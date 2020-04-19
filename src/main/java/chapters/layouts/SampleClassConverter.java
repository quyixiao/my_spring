package chapters.layouts;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SampleClassConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        StackTraceElement[] cda = event.getCallerData();
        if (cda != null && cda.length > 0) {
            String a = cda[0].getClassName();
            if(a.endsWith("LogUtils")){
                a =  cda[1].getClassName();
            }
            try {
                return a.substring(a.lastIndexOf(".") + 1);
            } catch (Exception e) {
                log.info(" SampleClassConverter exception className :", a);
            }
            return "";
        } else {
            return CallerData.NA;
        }
    }


} 