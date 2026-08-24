import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
     name='fyers_apiv3',  
     version='3.0.0',
     author="Fyers-Tech",
     author_email="support@fyers.in",
     description="Fyers trading APIs.",
     long_description=long_description,
     long_description_content_type="text/markdown",
     url="https://github.com/FyersDev/fyers-api-py",
     packages=setuptools.find_packages(),
     package_data={  
         'fyers_apiv3': ['/home/vinay/Documents/API-V3/Main-APIV3/fyers-api-py/fyers_apiv3/FyersWebsocket/map.json']
     },
     include_package_data=True,
     install_requires=[
                'requests==2.31.0',
                'asyncio==3.4.3',
                'aiohttp==3.8.4',
                'aws_lambda_powertools==1.25.5',
                'websocket-client==1.6.1',
          ],
     classifiers=[
         "Programming Language :: Python :: 3",
         "License :: OSI Approved :: MIT License",
         "Operating System :: OS Independent",
     ],
 )