A TeamCity plugin allowing you to customize TeamCity server pages by including some custom content or serving static pages separately.

Requirements:
=============
TeamCity 6.5 or newer

License:
========
Apache 2.0

Builds:
=======
Download the plugin from  http://teamcity.jetbrains.com/viewType.html?buildTypeId=bt334&tab=buildTypeStatusDiv

To build locally, open the project with IntelliJ IDEA, create the "plugin-zip" artifact. 
You may also need to set up the TeamCityDistribution path variable in IDEA to point to the
unpacked .exe or .tar.gz TeamCity distribution. 

Usage:
======

1) To include custom content as a part of server pages:

a. Locate the plugin configuration file under:

    [TeamCity Data Directory]/config/_static_ui_extensions/static-ui-extensions.xml


In the configuration file, specify the place on the page and the static file to be included.
HTML, css or js resources are supported. 


Here is a sample rule:
```xml
     <rule place-id="[put place id here]" 
           html-file="[include html file from this folder]"
           js-file="[include js file from this folder]"
           css-files="[include css file from this folder]">
         
         <!-- this is the rule to make page place work only for URLs that starts with -->
         <url starts="overview.html" />

         <!-- use empty path to match all pages -->
         <url starts="" />

         <!-- this is the rule to only include content if URL is equal to  -->
         <url equals="viewType.html" />

        You can add as many rules / url constraints as you like.
     </rule>
```

The full list of page extensions is available at:
http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/web/openapi/PlaceId.html

The full list of the supported page places can be found in the page-places-list.txt file generated automatically on the server start.


Sample usages:
==============
 - add google analytics to a TeamCity installation
 - add instance-specific info/announcement
 - patch TeamCity CSS/JS


2) To include static pages served separately

Place the content you want to serve into the

[TeamCity Data Directory]/config/_static_ui_extensions/pages directory.

The pages will be available under the /<TC context path>/app/static_content/

Sample usages:
==============
- add static html widgets, e.g. Top Investigations, Build Status etc/






