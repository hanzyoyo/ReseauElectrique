package fournisseurs;

import fournisseurs.FournisseurAgent.EnvoiGUI;
import fournisseurs.FournisseurAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TransportCheckBehaviour extends OneShotBehaviour {

	private double Somme;
	private FournisseurAgent myFournisseur;
	
	
	public TransportCheckBehaviour(double somme2, FournisseurAgent myFournisseur) {
		Somme = somme2;
		this.myFournisseur = myFournisseur;
	}


	public void action(){

		double conso_stat=Somme/12;

		Somme=0;
		myFournisseur.setb(false);

		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("electricity-transporter");
		template.addServices(sd);
		try{
			DFAgentDescription[] results = DFService.search(myAgent, template);

			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent("Demande Prix");
			msg.addReceiver(results[0].getName());
			myAgent.send(msg);
			MessageTemplate mt=MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchSender(results[0].getName()));

			/* step 1 j'�cris , step 2 je lis et block si rien, step 3 je traite (ou directement step 2)
			 * Pas OneShot mais Behaviour avec un done � la main */
			
			ACLMessage msgt=myAgent.receive(mt);
			if(msgt!=null){
				((FournisseurAgent) myAgent).setPrice_TIERS(Integer.valueOf(msgt.getContent()));
			}else{
				block();
			}

		}catch(FIPAException e){
			e.printStackTrace();
		}
		System.out.println("ON EST RENTRE !!!!!!!!!!!!!!!!!!!!!!!");
		/*d�cision myFournisseur.getLT()>deltat*/
		double seuil=12*myFournisseur.getLT()*Math.min(myFournisseur.getCapamoy()*(myFournisseur.getNb_transport_perso()+1),conso_stat)*myFournisseur.getPrice_TIERS()-myFournisseur.getCF();
		double deltat=(myFournisseur.getCF()/(Math.min(myFournisseur.getCapamoy(),conso_stat)*(myFournisseur.getPrice_TIERS())));
		if (seuil>0){myFournisseur.setNb_transport_perso(myFournisseur.getNb_transport_perso()+1);
		System.out.println("ATTENTION nouveau transport perso");
		
		}
		else {}
	}

}
