---
name: Bug Report EN
about: English template for bug reporting
title: ''
labels: 'Triage'
assignees: ''

---

<!-- This is an example comment and it won't be displayed -->

### I am running

* **Arclight** [VERSION] <!-- Versions are printed when Arclight is starting, for example arclight-1.15.2-1.0.3-SNAPSHOT-9455d03 -->

* **This is the latest development version** [Y/N, because ___]
<!-- Latest development build can be found at https://ci.appveyor.com/project/IzzelAliz/arclight/build/artifacts
     The issue you are reporting may be fixed
     If you are not running latest dev version, explain why -->

* **Java** [VERSION] <!-- Type java -version in your console -->

* **Operating System** [NAME & VERSION]

  <!-- Provide your plugins' versions if possible and this gives your report higher processing priority -->

* **Plugins** <!-- Run /plugins -->

* **Mods** <!-- Run /forge mods -->


### Description

<!-- Please include as much information as possible. For the description, assume we have no idea how 
        mods work, be as detailed as possible and include a step by step reproduction. It is recommended 
        you try to reproduce the issue you are having yourself with as few mods as possible. 
        The clearer the description, the higher the report processing priority -->

### Step to reproduce

1. Install something
2. ....
3. ....

<!-- (Optional) Server pack link: --> <!-- If you have too much mods/plugins included and you are not able to minimize the reproducible list, you can upload your server pack to GoogleDrive/Mega maybe. -->

<!-- If this is a mod related issue, test it in Forge without Arclight -->
**Reproducible in Forge** [Y/N]

<!-- If this is a plugin related issue, test it in Spigot without Arclight -->
**Reproducible in Spigot** [Y/N]

### Logs

[ERROR LOG]

<!-- Logs can be found in /logs/latest.log -->
<!-- After server is stopped, paste it to https://paste.ubuntu.com/ -->

<!-- If you have trouble using a pastebin, paste these codes to {ERROR LOG] and fill it

<details><pre>
[Logs here]
</pre></details>

-->