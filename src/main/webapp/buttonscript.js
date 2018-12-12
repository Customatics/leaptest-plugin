

function GetSch() {
          const leapworkHostname = document.getElementById("leapworkHostname").value;
          const leapworkPort = document.getElementById("leapworkPort").value;

          if(!leapworkHostname || !leapworkPort)
          {
            alert('"hostname or/and field is empty! Cannot connect to controller!"');
          }
          else
          {
              const address = "http://" + leapworkHostname + ":" + leapworkPort;
              const accessKey = document.getElementById("leapworkAccessKey").value;

              if(document.getElementById('LeapworkContainer').innerHTML == "")
              {

                  (jQuery).ajax({
                      url: address + "/api/v3/schedules",
                      headers: {'AccessKey': accessKey},
                      type: 'GET',
                      dataType:"json",
                      success: function(json)
                      {
                            const container = document.getElementById("LeapworkContainer");


                            (jQuery)(document).click(function (event) {
                                if ((jQuery)(event.target).closest('#LeapworkContainer').length == 0 && (jQuery)(event.target).attr('id') != 'mainButton') {
                                    (jQuery)("#LeapworkContainer input:checkbox").remove();
                                    (jQuery)("#LeapworkContainer li").remove();
                                    (jQuery)("#LeapworkContainer ul").remove();
                                    (jQuery)("#LeapworkContainer br").remove();
                                    container.style.display = 'none';
                                }
                            });

                            const schul = document.createElement('ul');
                            schul.className = 'ul-treefree ul-dropfree';

                            let schName = new Array();
                            let schId = new Array();
                            container.innerHTML += '<br>';

                            for (let i = 0; i < json.length; i++)
                            {

                                    schId.push(json[i].Id);
                                    schName.push(json[i].Title);

                                    let schli = document.createElement('li');
                                    let chbx = document.createElement('input');
                                    chbx.type = 'checkbox';
                                    chbx.name = schName[i];
                                    chbx.id = i;
                                    chbx.value = schId[i];

                                    if (json[i].IsEnabled != true)
                                    {
                                        chbx.disabled = true;
                                        schli.appendChild(chbx);
                                        schli.innerHTML+=schName[i].strike().italics().fontcolor("gray");
                                    }
                                    else
                                    {
                                        schli.appendChild(chbx);
                                        schli.innerHTML+=schName[i];
                                    }

                                    if(json[i].Type === "ScheduleInfo")
                                        schul.appendChild(schli);



                            }
                                     container.appendChild(schul);
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

                                     let TestNames = document.getElementById("schNames");
                                     let TestIds = document.getElementById("schIds");

                                     let boxes = (jQuery)("#LeapworkContainer input:checkbox");
                                     let existingTests = new Array();
                                     existingTests = TestNames.value.split("\n");

                                     if (TestNames.value != null && TestIds.value != null) {
                                            for (let i = 0; i < existingTests.length; i++) {
                                                for (j = 0; j < boxes.length; j++)
                                                {

                                                    if (existingTests[i] == boxes[j].getAttributeNode('name').value)
                                                     {
                                                        if(boxes[j].disabled == false)
                                                            (jQuery)(boxes[j]).prop('checked', 'checked');

                                                    }
                                                }
                                            }

                                     }

                                     (jQuery)("#LeapworkContainer input:checkbox").on("change", function ()
                                     {
                                         let NamesArray = new Array();
                                         let IdsArray = new Array();
                                         for (let i = 0; i < boxes.length; i++)
                                         {
                                              let box = boxes[i];
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

                      },
                      error: function(XMLHttpRequest, textStatus, errorThrown)
                      {
                                alert(
                                "Error occurred! Cannot get the list of Schedules!\n" +
                                "Status: " + textStatus + "\n" +
                                "Error: " + errorThrown + "\n" +
                                "This may occur because of the next reasons:\n" +
                                "1.Invalid controller hostname\n" +
                                "2.Invalid port number\n" +
                                "3.Invalid access key\n" +
                                "4.Controller is not running or updating now, check it in services\n" +
                                "5.Your Leapwork Controller API port is blocked.\nUse 'netstat -na | find \"9001\"' command, The result should be:\n 0.0.0.0:9001  0.0.0.0:0  LISTENING\n" +
                                "6.Your browser has such a setting enabled that blocks any http requests from https\n" +
                                "If nothing helps, please contact support https://leapwork.com/support"
                                );
                      }
                  });
              }
              else
              {
                  (jQuery)("#LeapworkContainer input:checkbox").remove();
                  (jQuery)("#LeapworkContainer li").remove();
                  (jQuery)("#LeapworkContainer ul").remove();
                  (jQuery)("#LeapworkContainer br").remove();
                  GetSch();
              }

          }
    }

