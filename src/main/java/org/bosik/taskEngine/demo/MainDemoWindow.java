package org.bosik.taskEngine.demo;

import org.bosik.taskEngine.core.MyExecutor;
import org.bosik.taskEngine.core.StrategyTemplate;
import org.bosik.taskEngine.core.Task;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

enum TaskType
{
	A, B;
}

/**
 * @author Nikita Bosik
 * @since 2017-09-18
 */
public class MainDemoWindow extends JFrame
{
	// CONSTANTS
	private static final String APP_TITLE  = "Tasks Demo";
	private static final int    BORDER_BIG = 10;

	// SYSTEM
	private final MyExecutor<TaskType, String> exec  = new MyExecutor<TaskType, String>((executor, task) ->
	{
		switch (task.getId())
		{
			case A:
			{
				return StrategyTemplate.<TaskType, String>singletonByIdAndParam().onBeforeSubmit(executor, task);
			}
			case B:
			{
				return StrategyTemplate.<TaskType, String>overrideById().onBeforeSubmit(executor, task);
			}
			default:
			{
				return true;
			}
		}
	});
	private final MyExecutor<TaskType, String> exec2 = new MyExecutor(StrategyTemplate.singletonByIdAndParam());

	// =========================================================================================

	public MainDemoWindow()
	{
		super(APP_TITLE);
		buildUi();
	}

	private void buildUi()
	{
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setContentPane(buildRootPanel());

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				exec.shutdownNow();
			}
		});

		pack();
		setMinimumSize(getSize());
		setLocationByPlatform(true);
	}

	private Container buildRootPanel()
	{
		return new JPanel()
		{
			{
				setLayout(new GridLayout(4, 1, BORDER_BIG, BORDER_BIG));

				add(new JButton("Task A(1)")
				{
					{
						addActionListener(e -> exec.submit(new ExampleTask(TaskType.A, "1")));
					}
				});

				add(new JButton("Task A(2)")
				{
					{
						addActionListener(e -> exec.submit(new ExampleTask(TaskType.A, "2")));
					}
				});

				add(new JButton("Task B(1)")
				{
					{
						addActionListener(e -> exec.submit(new ExampleTask(TaskType.B, "1")));
					}
				});

				add(new JButton("Task B(2)")
				{
					{
						addActionListener(e -> exec.submit(new ExampleTask(TaskType.B, "2")));
					}
				});
			}
		};
	}

	private static class ExampleTask extends Task<TaskType, String>
	{
		public ExampleTask(TaskType id, String param)
		{
			super(id, param);
		}

		@Override
		public void run()
		{
			try
			{
				System.out.println("Started: " + getId() + "(" + getParam() + ") " + this);
				Thread.sleep(2000);
				System.out.println("Finished: " + getId() + "(" + getParam() + ")" + this);
			}
			catch (InterruptedException e1)
			{
				System.err.println("\tInterrupted: " + getId() + "(" + getParam() + ")" + this);
			}
		}
	}
}