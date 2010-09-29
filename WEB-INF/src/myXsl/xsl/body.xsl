<?xml version="1.0" encoding="ISO-8859-1"?>
<stylesheet version="1.0" xmlns="http://www.w3.org/1999/XSL/Transform">

    <template match="/">

        <copy-of select="//div[@class='issues box']"/>
        <copy-of select="//div[@class='projects box']"/>
        <copy-of select="//div[@id='login-form']"/>

    </template>

</stylesheet>