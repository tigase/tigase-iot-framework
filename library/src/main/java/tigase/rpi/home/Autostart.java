package tigase.rpi.home;

/**
 * Created by andrzej on 22.10.2016.
 */

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Autostart {

}
