package gui;


import jade.core.Agent;
import jade.wrapper.StaleProxyException;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

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
		fenetre2.add(new JTextArea("Vous pouvez modifier ce texte",4,15));
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
			System.out.println("Nouveau Producteur");
		}
		
	}
	
	//listener who creates a new ClientAgent in the same controller when the button is clicked
	class NewClientListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				//TODO : how to create UNIQUE agent? no logs?
				getContainerController().createNewAgent("Bob", "client.ClientAgent", null);
			} catch (StaleProxyException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}
}
