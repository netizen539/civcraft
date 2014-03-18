/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.object;

/* Any object with a balance that can go into debt. */
public class EconObject {

	private String econName;
	private Double coins = 0.0;
	private Double debt = 0.0;
	private Double principalAmount = 0.0;
	private SQLObject holder;
	
	public EconObject(SQLObject holder) {
		this.holder = holder;
	}
	
	public String getEconomyName() {
		return econName;
	}
	
	public void setEconomyName(String name) {
		this.econName = name;
	}
	
	public double getBalance() {
		coins = Math.floor(coins);

		synchronized (coins) {
			return coins;
		}
	}
	
	
	public void setBalance(double amount) {
		this.setBalance(amount, true);
	}
	
	public void setBalance(double amount, boolean save) {
		if (amount < 0) {
			amount = 0;
		}
		amount = Math.floor(amount);
		
		synchronized (coins) {
			coins = amount;
		}
		
		if (save) {
			holder.save();
		}
	}
	
	public void deposit(double amount) {
		if (amount < 0) {
			amount = 0;
		}
		amount = Math.floor(amount);
		this.deposit(amount, true);
	}
	
	public void deposit(double amount, boolean save) {
		if (amount < 0) {
			amount = 0;
		}
		amount = Math.floor(amount);
		
		synchronized (coins) {
			coins += amount;
		}
		
		if (save) {
			holder.save();
		}

	}
	
	public void withdraw(double amount) {
		if (amount < 0) {
			amount = 0;
		}
		amount = Math.floor(amount);
		
		this.withdraw(amount, true);
	}
	
	public void withdraw(double amount, boolean save) {
		if (amount < 0) {
			amount = 0;
		}
		amount = Math.floor(amount);
		
		/*
		 * Update the principal we use to calculate interest,
		 * if our current balance dips below the principal,
		 * then we subtract from the principal.
		 */
		synchronized(principalAmount) {
			if (principalAmount > 0) {
				double currentBalance = this.getBalance();
				double diff = currentBalance - principalAmount;
				diff -= amount;
				
				if (diff < 0) {
					principalAmount -= (-diff);
				}
			}
		}
		
		synchronized(coins) {
			coins -= amount;
		}
		
		if (save) {
			holder.save();
		}
		
		
//		EconomyResponse resp;
//		resp = CivGlobal.econ.withdrawPlayer(getEconomyName(), amount);
//		if (resp.type == EconomyResponse.ResponseType.FAILURE) {
//			throw new EconomyException(resp.errorMessage);
//		}
	}
	
	public boolean hasEnough(double amount) {
		amount = Math.floor(amount);

		synchronized (coins) {
			if (coins >= amount) {
				return true;
			} else {
				return false;
			}
		}
	//	return CivGlobal.econ.has(getEconomyName(), amount);
	}
		
	public boolean payTo(EconObject objToPay, double amount) {
		if (!this.hasEnough(amount)) {
			return false;
		} else {
			this.withdraw(amount);
			objToPay.deposit(amount);
			return true;
		}
	}
	
	public double payToCreditor(EconObject objToPay, double amount) {
		double total = 0;
		
		if (this.hasEnough(amount)) {
			this.withdraw(amount);
			objToPay.deposit(amount);
			return amount;
		}
		
		/* Do not have enough to pay, pay what we can and put the rest into debt. */
		this.debt += amount - this.getBalance();
		objToPay.deposit(this.getBalance());
		this.withdraw(this.getBalance());
		
		return total;
	}
	
	
	public boolean inDebt() {
		debt = Math.floor(debt);

		if (debt > 0) {
			return true;
		}
		return false;
	}
	
	public double getDebt() {
		debt = Math.floor(debt);
		return debt;
	}

	public void setDebt(double debt) {
		debt = Math.floor(debt);
		this.debt = debt;
	}

	public double getPrincipalAmount() {
		return principalAmount;
	}

	public void setPrincipalAmount(double interestAmount) {
		this.principalAmount = interestAmount;
	}

	
}
