package com.github.irshulx.Components;

import android.content.Context;
import android.widget.TableLayout;
import android.widget.TextView;

import com.github.irshulx.BaseClass;
import com.github.irshulx.models.EditorContent;
import com.github.irshulx.models.EditorTextStyle;
import com.github.irshulx.models.EditorType;
import com.github.irshulx.models.HtmlTag;
import com.github.irshulx.models.state;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * Created by mkallingal on 5/25/2016.
 */
public class HTMLExtensions {
    private Context context;
    BaseClass base;

    public HTMLExtensions(BaseClass baseClass, Context context){
        this.base = baseClass;
        this.context = context;
    }
    public void parseHtml(String htmlString){
        Document doc= Jsoup.parse(htmlString);
        for (Element element:doc.body().children()){
            if(!matchesTag(element.tagName()))
                continue;
            buildNode(element);
        }
    }
    private void buildNode(Element element) {
        String text;
        TextView editText;
        HtmlTag tag= HtmlTag.valueOf(element.tagName().toLowerCase());
        int count= base.getParentView().getChildCount();
        if("<br>".equals(element.html().replaceAll("\\s+", ""))||"<br/>".equals(element.html().replaceAll("\\s+", ""))){
            base.getInputExtensions().InsertEditText(count, null, null);
            return;
        }
        else if("<hr>".equals(element.html().replaceAll("\\s+", ""))||"<hr/>".equals(element.html().replaceAll("\\s+", ""))){
            base.getDividerExtensions().InsertDivider();
            return;
        }
        switch (tag){
            case h1:
            case h2:
            case h3:
                RenderHeader(tag,element);
                break;
            case p:
                text= element.html();
                editText= base.getInputExtensions().InsertEditText(count, null, text);
                break;
            case ul:
            case ol:
                RenderList(tag==HtmlTag.ol, element);
                break;
            case img:
                RenderImage(element);
                break;
        }
    }

    private void RenderImage(Element element) {
        String src= element.attr("src");
        int Index= base.getParentChildCount();
        base.getImageExtensions().executeDownloadImageTask(src,Index);
    }
    private void RenderList(boolean isOrdered, Element element) {
        if(element.children().size()>0){
            Element li=element.child(0);
            String text=getHtmlSpan(li);
            TableLayout layout= base.getListItemExtensions().insertList(base.getParentChildCount(),isOrdered,text);
            for (int i=1;i<element.children().size();i++){
                 text=getHtmlSpan(li);
                base.getListItemExtensions().AddListItem(layout,isOrdered,text);
            }
        }
    }

    private void RenderHeader(HtmlTag tag, Element element){
       int count= base.getParentView().getChildCount();
       String text=getHtmlSpan(element);
       TextView  editText= base.getInputExtensions().InsertEditText(count, null, text);
       EditorTextStyle style= tag==HtmlTag.h1? EditorTextStyle.H1:tag==HtmlTag.h2? EditorTextStyle.H2: EditorTextStyle.H3;
       base.getInputExtensions().UpdateTextStyle(style,editText);
    }

    private String getHtmlSpan(Element element) {
        Element el = new Element(Tag.valueOf("span"), "");
        el.attributes().put("style",element.attr("style"));
        el.html(element.html());
        return el.toString();
    }

    private boolean hasChildren(Element element){
        return element.getAllElements().size() > 0;
    }


    private static boolean matchesTag(String test) {
        for (HtmlTag tag : HtmlTag.values()) {
            if (tag.name().equals(test)) {
                return true;
            }
        }
        return false;
    }

    private String getTemplateHtml(EditorType child){
        String template=null;
        switch (child){
            case INPUT:
                template= "<{{$tag}} {{$style}}>{{$content}}</{{$tag}}>";
                break;
            case hr:
                template="<hr/>";
                break;
            case img:
                template="<div><img src=\"{{$content}}\" /></div>";
                break;
            case map:
                template="<div><img src=\"{{$content}}\" /></div>";
                break;
            case ol:
                template="<ol>{{$content}}</ol>";
                break;
            case ul:
                template="<ul>{{$content}}</ul>";
                break;
            case OL_LI:
            case UL_LI:
                template="<li>{{$content}}</li>";
                break;
        }
        return template;
    }
    private String getInputHtml(state item){
        boolean isParagraph=true;
        String tmpl= getTemplateHtml(EditorType.INPUT);
      //  CharSequence content= android.text.Html.fromHtml(item.content.get(0)).toString();
      //  CharSequence trimmed= base.getInputExtensions().noTrailingwhiteLines(content);
        String trimmed= Jsoup.parse(item.content.get(0)).body().select("p").html();
        if(item._ControlStyles.size()>0) {
            for (EditorTextStyle style : item._ControlStyles) {
                switch (style) {
                    case BOLD:
                        tmpl = tmpl.replace("{{$content}}", "<b>{{$content}}</b>");
                        break;
                    case BOLDITALIC:
                        tmpl = tmpl.replace("{{$content}}", "<b><i>{{$content}}</i></b>");
                        break;
                    case ITALIC:
                        tmpl = tmpl.replace("{{$content}}", "<i>{{$content}}</i>");
                    case INDENT:
                        tmpl= tmpl.replace("{{$style}}","style=\"margin-left:25px\"");
                    case OUTDENT:
                        tmpl= tmpl.replace("{{$style}}","style=\"margin-left:0px\"");
                    case H1:
                        tmpl = tmpl.replace("{{$tag}}", "h1");
                        isParagraph = false;
                        break;
                    case H2:
                        tmpl = tmpl.replace("{{$tag}}", "h2");
                        isParagraph = false;
                        break;
                    case H3:
                        tmpl = tmpl.replace("{{$tag}}", "h3");
                        isParagraph = false;
                        break;
                    case NORMAL:
                        tmpl=tmpl.replace("{{$tag}}","p");
                        isParagraph=true;
                        break;
                }
            }
            if (isParagraph) {
                tmpl = tmpl.replace("{{$tag}}", "p");
            }
            tmpl=tmpl.replace("{{$content}}",trimmed);
            tmpl=tmpl.replace("{{$style}}","");
            return tmpl;
        }
            tmpl = tmpl.replace("{{$tag}}", "p");
        tmpl=tmpl.replace("{{$content}}",trimmed);
        tmpl=tmpl.replace(" {{$style}}","");
        return tmpl;
    }

    public String getContentAsHTML() {
        StringBuilder htmlBlock = new StringBuilder();
        String html;
        EditorContent content = base.getContent();
       return getContentAsHTML(content);
    }

    public String getContentAsHTML(EditorContent content){
        StringBuilder htmlBlock = new StringBuilder();
        String html;
        for (state item : content.stateList) {
            switch (item.type) {
                case INPUT:
                    html = getInputHtml(item);
                    htmlBlock.append(html);
                    break;
                case img:
                    htmlBlock.append(getTemplateHtml(item.type).replace("{{$content}}", item.content.get(0)));
                    break;
                case hr:
                    htmlBlock.append(getTemplateHtml(item.type));
                    break;
                case map:
                    htmlBlock.append(getTemplateHtml(item.type).replace("{{$content}}", item.content.get(0)));
                    break;
                case ul:
                case ol:
                    htmlBlock.append(getListAsHtml(item));
                    break;
            }
        }
        return htmlBlock.toString();
    }

    public String getContentAsHTML(String editorContentAsSerialized) {
        EditorContent content = base.getContentDeserialized(editorContentAsSerialized);
        return getContentAsHTML(content);
    }


    private String getListAsHtml(state item) {
        int count= item.content.size();
        String tmpl_parent = getTemplateHtml(item.type);
        StringBuilder childBlock=new StringBuilder();
        for (int i=0 ; i<count; i++){
            String tmpl_li= getTemplateHtml(item.type == EditorType.ul ? EditorType.UL_LI : EditorType.OL_LI);
            String trimmed= Jsoup.parse(item.content.get(i)).body().select("p").html();
            tmpl_li= tmpl_li.replace("{{$content}}",trimmed);
            childBlock.append(tmpl_li);
        }
       tmpl_parent= tmpl_parent.replace("{{$content}}",childBlock.toString());
        return tmpl_parent;
    }
}
