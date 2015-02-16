package gui;


import jade.core.Agent;
import jade.wrapper.StaleProxyException;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import client.*;
import fournisseurs.*;

public class GUI extends Agent{
	
	public void setup(){
		JFrame fenetre2 = new JFrame("Réseau Electrique");
		fenetre2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fenetre2.setPreferredSize(new Dimension(600, 400));

		fenetre2.setLayout(new GridLayout(2, 2));

		fenetre2.add(new JLabel("Producteurs"));
		
		//TODO : list producers and make them clickable to show their properties pane
		JPanel list_pane = new JPanel(); 
		list_pane.setLayout(new GridLayout(2, 1));
		
		JList<FournisseurAgent> list = new JList<FournisseurAgent>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JButton selectProdButton = new JButton("Afficher Détails");
		selectProdButton.addActionListener(new selectProdListener());
		
		list_pane.add(list);
		list_pane.add(selectProdButton);
		
		fenetre2.add(list_pane);
		//
		
		JButton newProdButton = new JButton("Nouveau Producteur");
		fenetre2.add(newProdButton);
		newProdButton.addActionListener(new NewProdListener());
		
		JButton newClientButton = new JButton(("Nouveau Client"));
		fenetre2.add(newClientButton);
		newClientButton.addActionListener(new NewClientListener());

		fenetre2.pack();
		fenetre2.setVisible(true);
	}

	class NewProdListener implements ActionListener{
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			//TODO : create new FournisseurAgent once added by Jean
			System.out.println("Nouveau Producteur");
		}
		
	}
	
	//listener who creates a new ClientAgent in the same controller when the button is clicked
	class NewClientListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				//TODO : how to create UNIQUE agent? no logs?
				Dialog dialog = new Dialog(super, "Nouvel Agent");
				JTextField agent_name = new JTextField("Nom de l'agent", 30);
				JButton confirm = new JButton("Confirmer");
				dialog.add(agent_name);
				dialog.add(confirm);
				
				getContainerController().createNewAgent("Bob", "client.ClientAgent", null);
			} catch (StaleProxyException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}
	
	class selectProdListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
