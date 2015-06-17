package osb.service.status.update;
import com.bea.wli.config.Ref;
import com.bea.wli.sb.management.configuration.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;
import javax.management.*;
import javax.management.remote.*;
import javax.naming.Context;
import weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean;

public class OSBServiceUpdate 
{
	public static void main(String[] args) 
	{
		System.out.println(changeProxyServiceStatus("ProxyService", true, "<SERVICE NAME>", "<IP>", 22011, "<USER>", "<PASSWORD>"));
	}

	public static String changeProxyServiceStatus(String servicetype, boolean status, String serviceURI, String host, int port, String username, String password) 
	{
		JMXConnector conn = null;
		SessionManagementMBean sm = null;
		String sessionName = "mysession";
		String statusmsg = "";
		try 
		{
			conn = initConnection(host, port, username, password);
			System.out.println("Connection OK");
			
			MBeanServerConnection mbconn = conn.getMBeanServerConnection();
			//DomainRuntimeServiceMBean domainService = MBeanServerInvocationHandler.newProxyInstance(mbconn, new ObjectName(DomainRuntimeServiceMBean.OBJECT_NAME), DomainRuntimeServiceMBean.class, status);
			DomainRuntimeServiceMBean domainService = (DomainRuntimeServiceMBean) weblogic.management.jmx.MBeanServerInvocationHandler.newProxyInstance(mbconn, new ObjectName(DomainRuntimeServiceMBean.OBJECT_NAME), DomainRuntimeServiceMBean.class, status);
			//SessionManagementMBean sm = JMX.newMBeanProxy(mbconn, ObjectName.getInstance(SessionManagementMBean.OBJECT_NAME), SessionManagementMBean.class);
			
			sm = (SessionManagementMBean) domainService.findService(SessionManagementMBean.NAME, SessionManagementMBean.TYPE, null);
			sm.createSession(sessionName);
			System.out.println("Create Session OK");
			
			ALSBConfigurationMBean alsbSession = (ALSBConfigurationMBean) domainService.findService(ALSBConfigurationMBean.NAME + "." + "mysession", ALSBConfigurationMBean.TYPE, null);
			if (servicetype.equals("ProxyService")) 
			{
				Ref ref = constructRef("ProxyService", serviceURI);
				ProxyServiceConfigurationMBean proxyConfigMBean = (ProxyServiceConfigurationMBean) domainService.findService(ProxyServiceConfigurationMBean.NAME + "." + sessionName, ProxyServiceConfigurationMBean.TYPE, null);
				if (status) 
				{
					proxyConfigMBean.enableMonitoring(ref);
					statusmsg = "Enabled the Service : " + serviceURI;
				} 
				else 
				{
					proxyConfigMBean.disableMonitoring(ref);
					statusmsg = "Disabled the Service : " + serviceURI;
				}
			} 
			else if (servicetype.equals("BusinessService")) 
			{
				Ref ref = constructRef("BusinessService", serviceURI);
				BusinessServiceConfigurationMBean businessConfigMBean = (BusinessServiceConfigurationMBean) domainService.findService(BusinessServiceConfigurationMBean.NAME+ "." + sessionName, BusinessServiceConfigurationMBean.TYPE, null);
				if (status) 
				{
			//		businessConfigMBean.enableService(ref);
					statusmsg = "Enabled Monitoring : " + serviceURI;
				} else {
			//		businessConfigMBean.disableService(ref);
					statusmsg = "Disabled Monitoring : " + serviceURI;
				}
			}
			sm.activateSession(sessionName, statusmsg);
			System.out.println("Session Activate");
			conn.close();
		
		} 
		catch (Exception ex) 
		{
			if (null != sm) 
			{
				try 
				{
					sm.discardSession(sessionName);
				} 
				catch (Exception e) 
				{
					System.out.println("able to discard the session");
				}
			}
			statusmsg = "Not able to perform the operation";
			ex.printStackTrace();
		} 
		finally 
		{
			if (null != conn)
				try 
				{
					conn.close();
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
		}
		return statusmsg;
	}
	
	private static JMXConnector initConnection(String hostname, int port, String username, String password) throws IOException,	MalformedURLException 
	{
		JMXServiceURL serviceURL = new JMXServiceURL("t3", hostname, port,"/jndi/" + DomainRuntimeServiceMBean.MBEANSERVER_JNDI_NAME);
		Hashtable<String, String> h = new Hashtable<String, String>();
		h.put(Context.SECURITY_PRINCIPAL, username);
		h.put(Context.SECURITY_CREDENTIALS, password);
		h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
		return JMXConnectorFactory.connect(serviceURL, h);
	}
	
	private static Ref constructRef(String refType, String serviceuri) 
	{
		Ref ref = null;
		String[] uriData = serviceuri.split("/");
		ref = new Ref(refType, uriData);
		return ref;
	}
	
}// End of class
