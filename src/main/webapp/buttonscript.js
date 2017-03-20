


 function GetSch() {
          if(!document.getElementsByName("address")[0].value)
          {
          alert('"Address field is empty! Cannot connect to server!"');
          }
          else
          {

            if((jQuery)("#MyContainer").html() == "")
            {
                var json;

                var url = document.getElementsByName("address")[0].value + "/api/v1/runSchedules";
                var XHR = ("onload" in new XMLHttpRequest()) ? XMLHttpRequest : XDomainRequest;
                var xhr = new XHR();
                xhr.open('GET', url, true);
                xhr.onload = function ()
                {


                    json = JSON.parse(this.responseText);

                    var container = document.getElementById("MyContainer");


                    (jQuery)(document).click(function (event) {
                        if ((jQuery)(event.target).closest('#MyContainer').length == 0 && (jQuery)(event.target).attr('id') != 'mainButton') {
                            (jQuery)("#MyContainer input:checkbox").remove();
                            (jQuery)("#MyContainer li").remove();
                            (jQuery)("#MyContainer ul").remove();
                            (jQuery)("#MyContainer br").remove();
                            container.style.display = 'none';


                        }
                    });


                    var schName = new Array();
                    var schId = new Array();
                    var schProjectId = new Array();

                    for (var i = 0; i < json.length; i++) {
                        if (json[i].IsDisplayedInScheduleList == true) {
                            schId.push(json[i].Id);
                            schName.push(json[i].Title);
                            schProjectId.push(json[i].ProjectId);
                        }
                    }

                    var projects = new Array();
                    var projurl = document.getElementsByName("address")[0].value + "/api/v1/Projects";
                    var XHRPr = ("onload" in new XMLHttpRequest()) ? XMLHttpRequest : XDomainRequest;
                    var xhrPr = new XHRPr();
                    xhrPr.open('GET', projurl, true);
                    xhrPr.onload = function ()
                    {

                        var projJson = JSON.parse(this.responseText);

                        for(var i = 0; i < projJson.length; i++)
                        {
                        projects.push(projJson[i].Title);
                        }

                        for(var i = 0; i < schProjectId.length; i++)
                        {
                            for(var j = 0; j < projJson.length; j++)
                            {

                                if(schProjectId[i] == projJson[j].Id)
                                {
                                    schProjectId[i] = projJson[j].Title;
                                }
                            }
                        }
                        projJson = null;

                        container.innerHTML += '<br>';

                        var drpdwn = document.createElement('ul');
                        drpdwn.className = 'ul-treefree ul-dropfree';

                        for(var i = 0; i < projects.length; i++)
                        {
                            var projectli = document.createElement('li');

                            var drop = document.createElement('div');
                            drop.class = 'drop';
                            drop.style = 'background-position: 0px 0px;';
                            projectli.appendChild(drop);
                            projectli.innerHTML+=projects[i];

                            var schul = document.createElement('ul');
                            schul.style = 'display:none;  font-weight: normal;';

                            for(var j = 0; j < schProjectId.length; j++)
                            {
                                if(projects[i] == schProjectId[j])
                                {
                                    var schli = document.createElement('li');
                                    var chbx = document.createElement('input');
                                    chbx.type = 'checkbox';
                                    chbx.name = schName[j];
                                    chbx.id = i;
                                    chbx.value = schId[j];

                                    schli.appendChild(chbx);
                                    schli.innerHTML+=schName[j];
                                    schul.appendChild(schli);
                                }
                            }

                            projectli.appendChild(schul);
                            drpdwn.appendChild(projectli);
                        }

                        container.appendChild(drpdwn);



                         container.innerHTML += '<br>';

                         container.style.display='block';

                         (jQuery)(".ul-dropfree").find("li:has(ul)").prepend('<div class="drop"></div>');
                         	(jQuery)(".ul-dropfree div.drop").click(function() {
                         		if ((jQuery)(this).nextAll("ul").css('display')=='none') {
                         			(jQuery)(this).nextAll("ul").slideDown(400);
                         			(jQuery)(this).css({'background-position':"-11px 0"});
                         		} else {
                         			(jQuery)(this).nextAll("ul").slideUp(400);
                         			(jQuery)(this).css({'background-position':"0 0"});
                         		}
                         	});
                         	(jQuery)(".ul-dropfree").find("ul").slideUp(400).parents("li").children("div.drop").css({'background-position':"0 0"});

                         var TestNames = document.getElementById("schNames");
                         var TestIds = document.getElementById("schIds");

                         var boxes = (jQuery)("#MyContainer input:checkbox");
                         var existingTests = new Array();
                         existingTests = TestNames.value.split("\n");

                            if (TestNames.value != null && TestIds.value != null) {
                                for (var i = 0; i < existingTests.length; i++) {
                                    for (j = 0; j < boxes.length; j++)
                                    {

                                        if (existingTests[i] == boxes[j].getAttributeNode('name').value)
                                         {
                                       (jQuery)(boxes[j]).prop('checked', 'checked');

                                        }
                                    }
                                }

                            }

                         (jQuery)("#MyContainer input:checkbox").on("change", function ()
                         {
                             var NamesArray = new Array();
                             var IdsArray = new Array();
                             for (var i = 0; i < boxes.length; i++)
                             {
                                  var box = boxes[i];
                                  if ((jQuery)(box).prop('checked'))
                                  {
                                        NamesArray[NamesArray.length] = (jQuery)(box).attr('name');
                                        IdsArray[IdsArray.length] = (jQuery)(box).val();
                                  }
                             }
                             TestNames.value = NamesArray.join("\n");
                             TestIds.value = IdsArray.join("\n");
                             console.log(TestIds.value)
                         });


                    }
                    xhrPr.onerror = function ()
                    {
                     alert('"Error occured! Cannot get the list of Projects! Check connection to your server!"' + this.status);
                    }
                     xhrPr.send();

                }
                xhr.onerror = function ()
                {
                    alert('"Error occured! Cannot get the list of schedules! Check connection to your server!"' + this.status);
                }
                xhr.send();
            }
            else
            {
                  (jQuery)("#MyContainer input:checkbox").remove();
                  (jQuery)("#MyContainer li").remove();
                  (jQuery)("#MyContainer ul").remove();
                  (jQuery)("#MyContainer br").remove();
                  GetSch();
            }
         }
    }


