const { JWT } = require('google-auth-library');
const AWS = require('aws-sdk');
const s3 = new AWS.S3();

exports.handler = async (event) => {
    try {
        const { Body } = await s3.getObject({ Bucket: process.env.BUCKET, Key: process.env.SERVICE_ACCOUNT_FILE_KEY }).promise();
        const keyFileContent = Body.toString('utf-8');
        const keyFileJSON = JSON.parse(keyFileContent);
        const scopes = ['https://www.googleapis.com/auth/drive.readonly'];
        const auth = new JWT({
            email: keyFileJSON.client_email,
            key: keyFileJSON.private_key,
            scopes
        });
        await auth.authorize();
        const accessToken = auth.credentials.access_token;
        return {
            statusCode: 200,
            body: accessToken,
        };

    } catch (error) {
        return {
            statusCode: 500,
            body: 'Error Generating OAuth 2.0 Token',
            reason: error
        };   
    }
};