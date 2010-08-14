/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.factories;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;

import org.apache.log4j.Logger;

/**
 * A class to find probe by their names
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProbeFactory {

	private final Logger logger = Logger.getLogger(ProbeFactory.class);
	final private List<String> probePackages = new ArrayList<String>(5);
	private Map<String, ProbeDesc> probeDescMap;
	private Map<String, GraphDesc> graphDescMap;
	private PropertiesManager pm;
	private Set<Class<?>> preloadedClass = new HashSet<Class<?>>();
	
	/**
	 * Private constructor
	 * @param b 
	 */
	public ProbeFactory(Map<String, ProbeDesc> probeDescMap, Map<String, GraphDesc> graphDescMap, PropertiesManager pm) {
		this.probeDescMap = probeDescMap;
		this.graphDescMap = graphDescMap;
		this.pm = pm;

		probePackages.add("");
	}

	/**
	 * Create an probe, provided his Class and a list of argument for a constructor
	 * for this object. It will be found using the default list of possible package
	 * @param className the probe name
	 * @param host 
	 * @param constArgs
	 * @return
	 */
	public  Probe<?,?> makeProbe(String probeType) {
		ProbeDesc pd = (ProbeDesc) probeDescMap.get(probeType);
		if(pd == null) {
			logger.error("Probe named " + probeType + " not found");
			return null;
		}
		Class<? extends Probe<?,?>> probeClass = pd.getProbeClass();
		if(probeClass == null) {
			logger.error("Invalid probe description " + probeType + ", probe class name not found");
		}
		String preload = pd.getPreloadClass();
		if(preload != null && ! "".equals(preload)) {
			try {
				preloadedClass.add(probeClass.getClassLoader().loadClass(preload));
			} catch (ClassNotFoundException e) {
				logger.error("Preload class " + preload + " for " + pd.getName() + "can 't be loaded");
			}
		}
		Probe<?,?> retValue = null;
		try {
			Constructor<? extends Probe<?,?>> c = probeClass.getConstructor();
			retValue = c.newInstance();
		}
		catch (LinkageError ex) {
			logger.warn("Error creating probe's " + pd.getName() +": " + ex);
			return null;
		}
		catch (ClassCastException ex) {
			logger.warn("didn't get a Probe but a " + retValue.getClass().getName());
			return null;
		} catch (Exception ex) {
			Throwable showException = ex;
			Throwable t = ex.getCause();
			if(t != null)
				showException = t;
			logger.warn("Error during probe instantation of type " + pd.getName() + ": ", showException);
			return null;
		}
		retValue.setPd(pd);
		return retValue;
	}

	public boolean configure(Probe<?, ?> p,  List<?> constArgs) {
		List<?> defaultsArgs = p.getPd().getDefaultArgs();
		if(defaultsArgs != null && constArgs != null && constArgs.size() <= 0)
			constArgs = defaultsArgs;
		Class<?>[] constArgsType = new Class[constArgs.size()];
		Object[] constArgsVal = new Object[constArgs.size()];
		int index = 0;
		for (Object arg: constArgs) {
			constArgsType[index] = arg.getClass();
			if(arg instanceof List<?>) {
				constArgsType[index] = List.class;
			}
			constArgsVal[index] = arg;
			index++;
		}
		try {
			Method configurator = p.getClass().getMethod("configure", constArgsType);
			Object result = configurator.invoke(p, constArgsVal);
			if(result != null && result instanceof Boolean) {
				if(logger.isTraceEnabled())
					logger.trace("Result of configuration for " + p + ": " + result);
				Boolean configured = (Boolean) result;
				if(! configured.booleanValue()) {
					return false;
				}
			}
			String name = p.getName();
			if(name == null)
				name = jrds.Util.parseTemplate(p.getPd().getProbeName(), p);
			p.setName(name);
			Collection<?> graphClasses = p.getPd().getGraphClasses();
			if(graphClasses != null) {
				for (Object o:  graphClasses ) {
					GraphDesc gd = graphDescMap.get(o);
					if(gd == null)
						continue;
					GraphNode newGraph = new GraphNode(p, gd);
					if(newGraph != null)
						p.addGraph(newGraph);
				}
			}
			if(pm != null) {
				logger.trace("Setting time step to " + pm.step + " for " + p);
				p.setStep(pm.step);
			}
			return true;
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
			logger.warn("ProbeDescription invalid " + p.getPd().getName() + ": no constructor " + e.getMessage() + " found");
			return false;
		}catch (Exception ex) {
			Throwable showException = ex;
			Throwable t = ex.getCause();
			if(t != null)
				showException = t;
			logger.warn("Error during probe creation of type " + p.getPd().getName() + " with args " + constArgs +
					": ", showException);
			return false;
		}
		return false;
	}

	public ProbeDesc getProbeDesc(String name) {
		return probeDescMap.get(name);
	}

	/**
	 * @return the preloadedClass
	 */
	public Set<Class<?>> getPreloadedClass() {
		return preloadedClass;
	}
}
