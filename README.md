
TTMLTransformer
===============


Introduction
----

TTMLTransformer is a custom transformer for Solr's DataImportHandler, used for indexing text stored in W3C's [Timed Text Markup Language] format.

Given an url containing valid TTML content, the transformer will extract the caption text to the same or a defined field.

For example, transforming the following 4 seconds of closed captioning from South Park episode 307:

```
<p begin="00:00:03.270" end="00:00:04.738" space="preserve">
<span style="block">
( Cartman doing menacing voice )
<br/>
HA HA HA HA...
</span>
</p>
<p begin="00:00:04.771" end="00:00:07.574" tts:textAlign="left" space="preserve">
<span style="block">
NOW I WILL KILL THE PRESIDENT
<br/>
AND KILL SALMA HAYEK !
</span>
</p>
```

would result in a field containing the following text: 

```( Cartman doing menacing voice ) HA HA HA HA...NOW I WILL KILL THE PRESIDENT AND KILL SALMA HAYEK !```


Configuration
----

Reference the TTMLTransformer and the RegexTransformer in the <entity> element's transformer attribute:

```<entity name="ent_name" transformer="RegexTransformer, com.mtvnet.search.solr.ext.TTMLTransformer">```



Add the TTML attribute to the field that will store the transformed TTML.

```<field column="TTML_field_name" sourceColName="ttml_url_field" TTML="text" />```


Alternatively, omit sourceColName and the ttml urls will be overwritten with the extracted caption text.

```<field column="new_single_valued_field" TML="text" />```



####A note about single- and mutli-valued fields:


If the urls field or sourceColName is multivalued, the resulting field should also be multivalued. Each url will be processed in order.


####Optional: Payloads

By setting the TTML value to "payloads" and an optional delimiter payload. The TTMLTransformer will created append the value of the begin parameter to each word in the transcript. Fields using this functionality should include [solr.DelimitedPayloadTokenFilterFactory] as part of their analysis chain.

[Timed Text Markup Language]:http://www.w3.org/TR/ttaf1-dfxp/
[solr.DelimitedPayloadTokenFilterFactory]: https://lucene.apache.org/core/4_0_0/analyzers-common/org/apache/lucene/analysis/payloads/DelimitedPayloadTokenFilterFactory.html

