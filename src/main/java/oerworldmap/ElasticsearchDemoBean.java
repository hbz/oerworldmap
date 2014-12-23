package oerworldmap;

import java.io.Serializable;

public class ElasticsearchDemoBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private int content;

    public ElasticsearchDemoBean(){
	this.content = 1;
    }
    
    public int getContent() {
	return content;
    }

    public void setContent(int content) {
	this.content = content;
    }
}
