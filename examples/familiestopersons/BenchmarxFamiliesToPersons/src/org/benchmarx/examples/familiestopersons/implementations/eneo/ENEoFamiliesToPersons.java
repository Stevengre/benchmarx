package org.benchmarx.examples.familiestopersons.implementations.eneo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.benchmarx.BXTool;
import org.benchmarx.config.Configurator;
import org.benchmarx.edit.ChangeAttribute;
import org.benchmarx.edit.CreateEdge;
import org.benchmarx.edit.CreateNode;
import org.benchmarx.edit.IEdit;
import org.benchmarx.eneo.f2p.F2P_GEN_InitiateSyncDialogue;
import org.benchmarx.eneo.f2p.F2P_MI;
import org.benchmarx.examples.familiestopersons.testsuite.Decisions;
import org.benchmarx.families.core.FamiliesComparator;
import org.benchmarx.persons.core.PersonsComparator;
import org.emoflon.neo.api.eneofamiliestopersons.API_Common;
import org.emoflon.neo.api.eneofamiliestopersons.org.benchmarx.eneo.f2p.API_Families;
import org.emoflon.neo.api.eneofamiliestopersons.org.benchmarx.eneo.f2p.API_Persons;
import org.emoflon.neo.api.eneofamiliestopersons.org.benchmarx.eneo.f2p.run.F2P_GEN_Run;

import Families.FamiliesFactory;
import Families.FamiliesPackage;
import Families.Family;
import Families.FamilyMember;
import Families.FamilyRegister;
import Persons.Female;
import Persons.Male;
import Persons.PersonRegister;
import Persons.PersonsFactory;
import Persons.PersonsPackage;

public class ENEoFamiliesToPersons implements BXTool<FamilyRegister, PersonRegister, Decisions> {
	private Configurator<Decisions> configurator;

	@Override
	public String getName() {
		return "eNeo";
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public void initiateSynchronisationDialogue() {
		try (var builder = API_Common.createBuilder()) {
			builder.clearDataBase();
			var gen = new F2P_GEN_InitiateSyncDialogue();
			gen.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void assertPostcondition(FamilyRegister source, PersonRegister target) {
		var actualSource = FamiliesFactory.eINSTANCE.createFamilyRegister();
		var actualTarget = PersonsFactory.eINSTANCE.createPersonRegister();

		try (var builder = API_Common.createBuilder()) {
			var familiesAPI = new API_Families(builder);
			var allFamiliesPattern = familiesAPI.getPattern_FamilyPattern();
			var allFamiliesMatches = allFamiliesPattern.pattern().determineMatches();

			allFamiliesMatches.forEach(fm -> {
				var f = allFamiliesPattern.data(List.of(fm)).findAny().get();

				var fc = FamiliesFactory.eINSTANCE.createFamily();
				fc.setName(f._family._name);
				actualSource.getFamilies().add(fc);

				{
					var pattern = familiesAPI.getPattern_MotherPattern();
					var mask = pattern.mask();
					mask.setFamily(fm.getElement(allFamiliesPattern._family));
					var allMothersMatches = pattern.determineMatches(mask);
					var allMothersData = pattern.data(allMothersMatches);
					allMothersData.forEach(m -> {
						var mc = FamiliesFactory.eINSTANCE.createFamilyMember();
						mc.setName(m._member._name);
						fc.setMother(mc);
					});
				}

				{
					var pattern = familiesAPI.getPattern_FatherPattern();
					var mask = pattern.mask();
					mask.setFamily(fm.getElement(allFamiliesPattern._family));
					var allFathersMatches = pattern.determineMatches(mask);
					var allFathersData = pattern.data(allFathersMatches);
					allFathersData.forEach(fa -> {
						var fac = FamiliesFactory.eINSTANCE.createFamilyMember();
						fac.setName(fa._member._name);
						fc.setFather(fac);
					});
				}

				{
					var pattern = familiesAPI.getPattern_DaughterPattern();
					var mask = pattern.mask();
					mask.setFamily(fm.getElement(allFamiliesPattern._family));
					var allDaughtersMatches = pattern.determineMatches(mask);
					var allDaughtersData = familiesAPI.getPattern_DaughterPattern().data(allDaughtersMatches);
					allDaughtersData.forEach(d -> {
						var dc = FamiliesFactory.eINSTANCE.createFamilyMember();
						dc.setName(d._member._name);
						fc.getDaughters().add(dc);
					});
				}

				{
					var pattern = familiesAPI.getPattern_SonPattern();
					var mask = pattern.mask();
					mask.setFamily(fm.getElement(allFamiliesPattern._family));
					var allSonsMatches = pattern.determineMatches(mask);
					var allSonsData = familiesAPI.getPattern_SonPattern().data(allSonsMatches);
					allSonsData.forEach(s -> {
						var sc = FamiliesFactory.eINSTANCE.createFamilyMember();
						sc.setName(s._member._name);
						fc.getSons().add(sc);
					});
				}
			});

			var formatter = new SimpleDateFormat("yyyy-MM-dd");
			var dateTimeFormatter = DateTimeFormatter.ISO_DATE;
			var personsAPI = new API_Persons(builder);
			var allFemalesMatches = personsAPI.getPattern_FemalePattern().pattern().determineMatches();
			var allFemalesData = personsAPI.getPattern_FemalePattern().data(allFemalesMatches);
			allFemalesData.forEach(fp -> {
				var fpc = PersonsFactory.eINSTANCE.createFemale();
				if (fp._person._birthday != null)
					try {
						fpc.setBirthday(formatter.parse(fp._person._birthday.format(dateTimeFormatter)));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				fpc.setName(fp._person._name);
				actualTarget.getPersons().add(fpc);
			});
			var allMalesMatches = personsAPI.getPattern_MalePattern().pattern().determineMatches();
			var allMalesData = personsAPI.getPattern_MalePattern().data(allMalesMatches);
			allMalesData.forEach(mp -> {
				var mpc = PersonsFactory.eINSTANCE.createMale();
				if (mp._person._birthday != null)
					try {
						mpc.setBirthday(formatter.parse(mp._person._birthday.format(dateTimeFormatter)));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				mpc.setName(mp._person._name);
				actualTarget.getPersons().add(mpc);
			});
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		new FamiliesComparator().assertEquals(source, actualSource);
		new PersonsComparator().assertEquals(target, actualTarget);
	}

	@Override
	public void assertPrecondition(FamilyRegister source, PersonRegister target) {
		assertPostcondition(source, target);
	}

	@Override
	public void performAndPropagateEdit(Supplier<IEdit<FamilyRegister>> sourceEdit,
			Supplier<IEdit<PersonRegister>> targetEdit) {
		try (var builder = API_Common.createBuilder()) {
			var familyAPI = new API_Families(builder);
			var personsAPI = new API_Persons(builder);

			for (var s : sourceEdit.get().getSteps()) {
				if (s instanceof CreateNode) {
					var cn = (CreateNode<FamilyRegister>) s;
					if (cn.getNode() instanceof Family) {
						var family = (Family) cn.getNode();
						var rule = familyAPI.getRule_CreateFamily();
						var mask = rule.mask();
						mask.addParameter(rule._param__name, family.getName());
						mask.addParameter(rule._param__id, family.hashCode());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						rule.apply(mask, mask);
					} else if (cn.getNode() instanceof FamilyMember) {
						var member = (FamilyMember) cn.getNode();
						var rule = familyAPI.getRule_CreateFamilyMember();
						var mask = rule.mask();
						mask.addParameter(rule._param__name, member.getName());
						mask.addParameter(rule._param__id, member.hashCode());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						rule.apply(mask, mask);
					} else {
						throw new IllegalArgumentException("Unable to handle created node: " + cn.getNode());
					}
				} else if (s instanceof CreateEdge) {
					var ce = (CreateEdge<FamilyRegister>) s;
					if (ce.getType().equals(FamiliesPackage.Literals.FAMILY_REGISTER__FAMILIES)) {
						var rule = familyAPI.getRule_CreateRegisterFamilyEdge();
						var family = (Family) ce.getTarget();
						var mask = rule.mask();
						mask.addParameter(rule._param__name, family.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__id, family.hashCode());
						rule.apply(mask, mask);
					} else if (ce.getType().equals(FamiliesPackage.Literals.FAMILY__SONS)) {
						var rule = familyAPI.getRule_CreateFamilySonEdge();
						var mask = rule.mask();
						var family = (Family) ce.getSource();
						mask.addParameter(rule._param__fname, family.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__fid, family.hashCode());
						var member = (FamilyMember) ce.getTarget();
						mask.addParameter(rule._param__name, member.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__id, member.hashCode());
						rule.apply(mask, mask);
					} else if (ce.getType().equals(FamiliesPackage.Literals.FAMILY__DAUGHTERS)) {
						var rule = familyAPI.getRule_CreateFamilyDaughterEdge();
						var mask = rule.mask();
						var family = (Family) ce.getSource();
						mask.addParameter(rule._param__fname, family.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__fid, family.hashCode());
						var member = (FamilyMember) ce.getTarget();
						mask.addParameter(rule._param__name, member.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__id, member.hashCode());
						rule.apply(mask, mask);
					} else if (ce.getType().equals(FamiliesPackage.Literals.FAMILY__MOTHER)) {
						var rule = familyAPI.getRule_CreateFamilyMotherEdge();
						var mask = rule.mask();
						var family = (Family) ce.getSource();
						mask.addParameter(rule._param__fname, family.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__fid, family.hashCode());
						var member = (FamilyMember) ce.getTarget();
						mask.addParameter(rule._param__name, member.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__id, member.hashCode());
						rule.apply(mask, mask);
					} else if (ce.getType().equals(FamiliesPackage.Literals.FAMILY__FATHER)) {
						var rule = familyAPI.getRule_CreateFamilyFatherEdge();
						var mask = rule.mask();
						var family = (Family) ce.getSource();
						mask.addParameter(rule._param__fname, family.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__fid, family.hashCode());
						var member = (FamilyMember) ce.getTarget();
						mask.addParameter(rule._param__name, member.getName());
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.SRC_MODEL_NAME);
						mask.addParameter(rule._param__id, member.hashCode());
						rule.apply(mask, mask);
					} else {
						throw new IllegalArgumentException("Unable to handle created edge: " + ce.getType());
					}
				} else if (s instanceof ChangeAttribute) {
					var ca = (ChangeAttribute<FamilyRegister>) s;
					if (ca.getAttribute() == FamiliesPackage.Literals.FAMILY__NAME) {
						var rule = familyAPI.getRule_ChangeNameOfFamily();
						var mask = rule.mask();
						mask.addParameter(rule._param__name, ca.getNewValue());
						mask.addParameter(rule._param__id, ca.getNode().hashCode());
						rule.apply(mask, mask);
					} else {
						throw new IllegalArgumentException("Unable to handle change attribute: " + ca.getAttribute());
					}
				} else {
					throw new IllegalArgumentException("Unable to handle atomic edit: " + s);
				}
			}
			for (var t : targetEdit.get().getSteps()) {
				if (t instanceof CreateNode) {
					var cn = (CreateNode<PersonRegister>) t;

					if (cn.getNode() instanceof Male) {
						var p = (Male) cn.getNode();
						var rule = personsAPI.getRule_CreateMale();
						var mask = rule.mask();
						mask.addParameter(rule._param__name, p.getName());
						mask.addParameter(rule._param__id, p.hashCode());
						mask.addParameter(rule._param__bday,
								LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(p.getBirthday())));
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.TRG_MODEL_NAME);
						rule.apply(mask, mask);
					} else if (cn.getNode() instanceof Female) {
						var p = (Female) cn.getNode();
						var rule = personsAPI.getRule_CreateFemale();
						var mask = rule.mask();
						mask.addParameter(rule._param__name, p.getName());
						mask.addParameter(rule._param__id, p.hashCode());
						mask.addParameter(rule._param__bday,
								LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(p.getBirthday())));
						mask.addParameter(rule._param__namespace, F2P_GEN_Run.TRG_MODEL_NAME);
						rule.apply(mask, mask);
					} else {
						throw new IllegalArgumentException("Unable to handle created node: " + cn.getNode());
					}
				} else if (t instanceof CreateEdge) {
					var ce = (CreateEdge<PersonRegister>) t;
					if (ce.getType().equals(PersonsPackage.Literals.PERSON_REGISTER__PERSONS)) {
						var rule = personsAPI.getRule_CreateRegisterPersonEdge();
						var mask = rule.mask();
						mask.addParameter(rule._param__id, ce.getTarget().hashCode());
						rule.apply(mask, mask);
					}
				} else if (t instanceof ChangeAttribute) {
					// FIXME
					// Change attribute: birthday, name
				} else {
					throw new IllegalArgumentException("Unable to handle atomic edit: " + t);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (configurator != null) {
				var mi = new F2P_MI(Optional.of(configurator.decide(Decisions.PREFER_CREATING_PARENT_TO_CHILD)),
						Optional.of(configurator.decide(Decisions.PREFER_EXISTING_FAMILY_TO_NEW)));
				mi.runModelIntegration();
			} else {
				var mi = new F2P_MI(Optional.empty(), Optional.empty());
				mi.runModelIntegration();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void performIdleSourceEdit(Supplier<IEdit<FamilyRegister>> edit) {
		performAndPropagateSourceEdit(edit);
	}

	@Override
	public void performIdleTargetEdit(Supplier<IEdit<PersonRegister>> edit) {
		performAndPropagateTargetEdit(edit);
	}

	@Override
	public void setConfigurator(Configurator<Decisions> configurator) {
		this.configurator = configurator;
	}

	@Override
	public FamilyRegister getSourceModel() {
		return FamiliesFactory.eINSTANCE.createFamilyRegister();
	}

	@Override
	public PersonRegister getTargetModel() {
		return PersonsFactory.eINSTANCE.createPersonRegister();
	}
}
