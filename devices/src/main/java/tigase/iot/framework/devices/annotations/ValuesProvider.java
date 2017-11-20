package tigase.iot.framework.devices.annotations;

import java.lang.annotation.*;

/**
 * Annotation for a fields annotated with <code>@ConfigField</code> annotation marking this fields as a field with a
 * list of possible values returned by a bean with name specified in <code>beanName</code> property of this annotation.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ValuesProvider {

	String beanName();

}
