function loadList() {
  fetch("/modlist.json")
    .then(res => res.json())
    .then(onListLoaded)
    .catch(err => { throw err; });
}

function onListLoaded(json) {
  var table = document.querySelector("#modlist > tbody");
  var rowTemplate = document.querySelector("#modlist > template#row");
  var dlTemplates = {
    "curse":    document.querySelector("#modlist > template#dl-curse"),
    "modrinth": document.querySelector("#modlist > template#dl-modrinth")
  };
  var loading = document.querySelector("#loading");
  
  loading.parentNode.removeChild(loading);
  
  for (let mod of json.mods) {
    var row = rowTemplate.content.cloneNode(true);
    var columns = row.querySelectorAll("td");
    columns[1].innerHTML = mod.name + (mod.summary && "<br /><small style='font-weight: normal; font-size: smaller; color: rgba(0,0,0,0.5)'>" + mod.summary + "</small>");
    columns[1].title = "Mod ID: " + mod.id;
    columns[2].textContent = mod.version;
    columns[3].textContent = mod.authors ?? ""
    if (mod.download) {
      var download = dlTemplates[mod.download.type].content.cloneNode(true);
      download.href = mod.download.url;
      columns[0].appendChild(download);
    }
    table.appendChild(row);
  }
}
