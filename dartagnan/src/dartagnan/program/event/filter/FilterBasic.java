package dartagnan.program.event.filter;

import dartagnan.program.event.Event;

public class FilterBasic extends FilterAbstract {

    private String param;

    public FilterBasic(String param){
        this.param = param;
    }

    @Override
    public boolean filter(Event e){
        return e.is(param);
    }

    @Override
    public String toString(){
        return param;
    }

    @Override
    public Integer toRepositoryCode(){
        return FilterUtils.toRepositoryCode.get(param);
    }
}
