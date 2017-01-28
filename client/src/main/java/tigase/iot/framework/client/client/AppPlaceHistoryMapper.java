/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import tigase.iot.framework.client.client.auth.AuthPlace;
import tigase.iot.framework.client.client.devices.DevicesListPlace;

/**
 *
 * @author andrzej
 */
@WithTokenizers({AuthPlace.Tokenizer.class,DevicesListPlace.Tokenizer.class})
public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {      
        
}
