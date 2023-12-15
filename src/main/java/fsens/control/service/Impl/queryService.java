package fsens.control.service.Impl;

import fsens.control.annotation.myService;
import fsens.control.service.MyService;
import fsens.model.User;

@myService
public class queryService implements MyService {
    public String query(String info){
        if(info.equalsIgnoreCase( "name")){
            return new User().getName();
        }
        else if(info.equalsIgnoreCase("sex")){
            return new User().getSex();
        }
        else if(info.equalsIgnoreCase("age")){
            return new User().getAge();
        }
        return null;
    }

}
