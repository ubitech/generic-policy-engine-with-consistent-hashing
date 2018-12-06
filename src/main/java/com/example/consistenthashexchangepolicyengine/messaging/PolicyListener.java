/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.consistenthashexchangepolicyengine.messaging;

import static com.example.consistenthashexchangepolicyengine.Util.createKjar;
import static com.example.consistenthashexchangepolicyengine.Util.deployKjar;
import com.example.consistenthashexchangepolicyengine.KieContainersManagement.KieUtil;
import com.example.consistenthashexchangepolicyengine.facts.MonitoredComponent;
import com.example.consistenthashexchangepolicyengine.facts.SampleFact;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.EntryPoint;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Eleni Fotopoulou <efotopoulou@ubitech.eu>
 */
@Component
public class PolicyListener {

    private static Logger log = LoggerFactory.getLogger(PolicyListener.class);

    @Autowired
    private KieUtil kieUtil;

    @RabbitListener(queues = "#{autoDeleteQueue.name}")
    public void policyMessageReceived(byte[] message) {

        String messageToString = new String(message, StandardCharsets.UTF_8);
        log.info("I received a new message ");

        JSONObject messageToJSON = new JSONObject(messageToString);

        String polic_action = messageToJSON.getString("policy_action");

        switch (polic_action) {
            case "enforce_policy":
                enforce_policy(messageToJSON);
                break;
            case "update_policy":
                update_policy(messageToJSON);
                break;
            case "get_monitoring_alert":
                get_monitoring_alert(messageToJSON);
                break;
            default:
                break;
        }

    }

    /*
     content_type: application/json
     message_id: "my-x-kjar"
     {
     "policy_action":"enforce_policy",
     "deployed_graph": "my-x-kjar",
     "rules": "package rules.package1;\n import com.example.consistenthashexchangepolicyengine.facts.*\n declare  MonitoredComponent \n  @expires( 5m )\n  @role( event )\n end\n rule \"My First policy_name Rule\"\n when\n $o: Object()\n then\n System.out.println(\" >>> Rule Fired for Object policy_name changed: \"+$o.toString());\n end\n rule \"My Second policy_name Rule\"\n when\n        $tot0 := java.lang.Double( $tot0 >70.0 ) from accumulate($m0 := MonitoredComponent( name== \"vnf1\" && metric== \"CPULoad\" ) over window:time(70s)from entry-point \"MonitoringStream\" ,\n        average( $m0.getValue() )  )\n     then\n System.out.println(\" >>> Rule Fired for MonitoredComponent policy example\");\n end\n\n rule \"My Third policy_name Rule\"\n when\n $sampleFact: SampleFact()\n then\n $sampleFact.dosomething($sampleFact.getValue());\n end"
     }
     */
    private void enforce_policy(JSONObject messageToJSON) {

        log.info("Enforcement of New Policy Message Received: " + messageToJSON.toString());

        //deploy kjar to nexus
        String groupId = "generic.policy.rule";
        String version = "1.0.0-SNAPSHOT";

        String artifactId = messageToJSON.getString("deployed_graph");
        String rules = messageToJSON.getString("rules");
        createKjar(groupId, artifactId, version, rules);
        boolean succesful_deploy = deployKjar(artifactId);

        if (!succesful_deploy) {
            return;
        }

        //enforce policy
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId2 = ks.newReleaseId(groupId, artifactId, version);
        KieContainer kcontainer2 = ks.newKieContainer(releaseId2);
        KieScanner kscanner2 = ks.newKieScanner(kcontainer2);

        kscanner2.start(5000);

        String sessionname = artifactId;
        final KieSession ksession = kcontainer2.newKieSession(sessionname);

        kieUtil.fireKieSession(ksession, sessionname);
    }

    /*
     content_type: application/json
     message_id: "my-x-kjar"
    
     {
     "policy_action":"update_policy",
     "deployed_graph": "my-x-kjar",
     "rules": "package rules.package1;\n import com.example.consistenthashexchangepolicyengine.facts.*\n declare  MonitoredComponent \n  @expires( 5m )\n  @role( event )\n end\n rule \"My First policy_name Rule\"\n when\n $o: Object()\n then\n System.out.println(\" >>> Rule Fired for Object policy_name changed: \"+$o.toString());\n end\n rule \"My Second policy_name Rule\"\n when\n        $tot0 := java.lang.Double( $tot0 >170.0 ) from accumulate($m0 := MonitoredComponent( name== \"vnf1\" && metric== \"CPULoad\" ) over window:time(70s)from entry-point \"MonitoringStream\" ,\n        average( $m0.getValue() )  )\n     then\n System.out.println(\" >>> Rule Fired for MonitoredComponent policy example\");\n end\n\n rule \"My Third policy_name Rule\"\n when\n $sampleFact: SampleFact()\n then\n $sampleFact.dosomething($sampleFact.getValue());\n end"
     }
    
     */
    private void update_policy(JSONObject policyUpdateInfoJson) {

        log.info("New updatePolicy Message Received: " + policyUpdateInfoJson.toString());

        String groupId = "generic.policy.rule";
        String version = "1.0.0-SNAPSHOT";
        String artifactId = policyUpdateInfoJson.getString("deployed_graph");

        Path artifactIdPath = Paths.get(artifactId);

        if (Files.exists(artifactIdPath)) {
            String rules = policyUpdateInfoJson.getString("rules");

            createKjar(groupId, artifactId, version, rules);
            boolean succesful_deploy = deployKjar(artifactId);

            if (!succesful_deploy) {
                return;
            }
            
        } else {
            log.info("update policy message ignored");
        }

    }

    /*
     {
     "policy_action":"get_monitoring_alert",
     "ObjectType": "SampleFact",
     "deployed_graph":"my-x-kjar",
     "ObjectData":{"value": "SampleFactValue","deployed_graph":"my-x-kjar"}
     }
    
     content_type: application/json
     message_id: "my-x-kjar"
     {
     "policy_action":"get_monitoring_alert",
     "ObjectType": "MonitoredComponent",
     "deployed_graph":"my-x-kjar",
     "ObjectData":{"name": "vnf1","metric":"CPULoad","value":80,"deployed_graph":"my-x-kjar"}
     }
     */
    private void get_monitoring_alert(JSONObject messageToJSON) {

        log.info("New Monitoring Message: " + messageToJSON.toString());

        String objectType = messageToJSON.getString("ObjectType");
        String containerName = messageToJSON.getString("deployed_graph");

        if (!kieUtil.seeThreadMap().containsKey(containerName)) {
            log.info("Missing Knowledge base " + containerName);
            return;
        }

        String objectAsString = messageToJSON.getJSONObject("ObjectData").toString();
        KieSession ksession = (KieSession) kieUtil.seeThreadMap().get(containerName);

        switch (objectType) {
            case "SampleFact":
                SampleFact sampleFact = new Gson().fromJson(objectAsString, SampleFact.class);
                //System.out.println("convertStringToObject result for sample fact object " + sampleFact.getValue());
                ksession.insert(sampleFact);
                break;
            case "MonitoredComponent":
                MonitoredComponent component = new Gson().fromJson(objectAsString, MonitoredComponent.class);
                EntryPoint monitoringStream = ksession.getEntryPoint("MonitoringStream");
                monitoringStream.insert(component);
                break;
            default:
                break;
        }

    }

}
